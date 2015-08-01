import groovy.json.JsonSlurper
import groovy.transform.InheritConstructors

ext.ROOT_PACKAGE = 'de.johni0702.mc.protocolgen'

task genPacketSources << {
    def jsonSlurper = new JsonSlurper()
    def jsonString = 'https://raw.githubusercontent.com/PrismarineJS/minecraft-data/snapshot-1.9/enums/protocol.json'.toURL().text
    // ugly hack to prevent uppercase field names
    jsonString = jsonString.replace('"UUID"', '"uuid"')
    def root = jsonSlurper.parseText(jsonString)

    root.each {
        def pckg = ROOT_PACKAGE + '.' + it.key
        def name = it.key.capitalize()
        generateProtocol('Server' + name, pckg + '.server', it.value.toServer)
        generateProtocol('Client' + name, pckg + '.client', it.value.toClient)
    }

}

def generateProtocol(protocolName, pckg, root) {
    def folder = file('src/gen/java/' + pckg.replace('.', '/'))
    if (!folder.exists()) folder.mkdirs()
    protocolName = 'Protocol' + protocolName
    def f = new File(folder, protocolName + '.java')
    if (f.exists()) f.delete()

    f << """// This file was generated automatically. Do not edit.
package $pckg;

import ${ROOT_PACKAGE}.Packet;

import java.util.HashMap;

public class $protocolName extends HashMap<Integer, Class<? extends Packet>> {

\tpublic $protocolName() {
"""

    root.sort{ it.value.id }.each {
        def name = 'Packet' + it.key.capitalize().replaceAll(/_\w/){ it[1].toUpperCase() }
        generatePacket(pckg, name, it.value)
        f << "\t\tput($it.value.id, ${name}.class);\n"
    }

    f << '\t}\n}\n'
}

def generatePacket(pckg, name, root) {
    println "Generating $pckg.$name"
    def folder = file('src/gen/java/' + pckg.replace('.', '/'))
    if (!folder.exists()) folder.mkdirs()
    def f = new File(folder, name + '.java')
    if (f.exists()) f.delete()

    def containers = new HashSet<ContainerField>()
    def fields = Parser.parseFields(root.fields, [:], new HashSet<>(), null, containers)
    containers.each { it.parseClass(fields) }
    generatePacketHeader(f, pckg, name)
    Parser.generateBody(f, '\t', fields, Collections.emptySet(), containers)
    generatePacketFooter(f)
}

class Parser {
    static Map<String, Field> parseFields(root, Map<String, Field> parentFields, Set<Field> referencedFields, Field parent, Set<ContainerField> containers) {
        def fields = [:]
        root.each {
            def referenced = new HashSet()
            def field = parseField(it, parent, fields + parentFields, referenced, containers)
            referencedFields.addAll(referenced)
            fields.put(it.name, field)
        }
        return fields
    }

    static Field parseField(it, Field parent, Map<String, Field> fields, Set<Field> referenced, Set<ContainerField> containers) {
        switch (it.type) {
            case 'count':
                def field = new CountField(parent, it.name)
                field.child = parseField(it.typeArgs, field, fields, referenced, containers)
                return field
            case 'buffer':
                Field count
                CountTransformation countTransformation
                (count, countTransformation) = parseCount(it.typeArgs.count, fields)
                referenced.add(count)
                def field = new BufferField(parent, it.name, count, countTransformation)
                return field
            case 'condition':
                def field = it
                def conditions = it.typeArgs.values.collect {
                    if (it instanceof String) {
                        /"$it".equals($field.typeArgs.field)/
                    } else {
                        "$field.typeArgs.field == $it"
                    }
                }
                if (it.typeArgs.different as boolean) {
                    conditions = conditions.collect { "!($it)" }
                }
                referenced.add(fields.get(it.typeArgs.field))
                field = new ConditionField(parent, it.name, conditions.join(' || '))
                field.child = parseField(it.typeArgs, field, fields, referenced, containers)
                return field
            case 'array':
                Field count
                CountTransformation countTransformation
                (count, countTransformation) = parseCount(it.typeArgs.count, fields)
                referenced.add(count)
                def field = new ArrayField(parent, it.name, count, countTransformation)
                field.child = parseField(it.typeArgs, field, fields, referenced, containers)
                return field
            case 'container':
                def field = new ContainerField(parent, it.name, it.typeArgs.fields)
                containers.add(field)
                return field
            default:
                return new SimpleField(parent, it.type, it.name)
        }
    }

    static def parseCount(root, fields) {
        CountTransformation transform
        def count
        if (root instanceof String) {
            transform = new NoCountTransformation()
            count = root
        } else {
            transform = new SwitchTransformation(root.default, root.map)
            count = root.field
        }
        if (count.startsWith("this.")) {
            count = count.substring("this.".length())
        }
        Field countField = fields.get(count)
        if (countField == null) {
            throw new InvalidUserDataException("Can't find count field '$count'")
        }
        return [countField, transform]
    }

    static def generateBody(File f, String indent, Map<String, Field> fields, Set<Field> referenced, Set<ContainerField> containers) {
        // Fields
        fields.values().each { it.generateDeclaration(f, indent) }
        f << '\n'

        // Read
        def args = referenced.collect { ", $it.javaType $it.name" }.join('')
        f << indent + (indent.length() == 1 ? 'public' : 'private') + " void read(NetInput in$args) throws IOException {\n"
        fields.values().each { it.generateLocalDeclaration(f, indent + '\t') }
        fields.values().each { it.generateRead(f, indent + '\t', it.name) }
        f << indent + '}\n'
        f << '\n'

        // Write
        args = referenced.collect { ", $it.javaType $it.name" }.join('')
        f << indent + (indent.length() == 1 ? 'public' : 'private') + " void write(NetOutput out$args) throws IOException {\n"
        fields.values().each { it.generateWrite(f, indent + '\t', it.name) }
        f << indent + '}\n'
        f << '\n'

        // Containers / Inner classes
        containers.each { it.generateClass(f, indent) }
    }
}

abstract class Field {
    Field parent
    Field child
    String name

    Field(Field parent, String name) {
        this.parent = parent
        this.name = name
    }

    String getJavaType() {
        parent?.javaType
    }

    String getName() {
        name != null ? name : parent?.getName()
    }

    abstract void generateDeclaration(File file, String indent)
    abstract void generateLocalDeclaration(File file, String indent)
    abstract void generateRead(File file, String indent, String name)
    abstract void generateWrite(File file, String indent, String name)
}

class SimpleField extends Field {
    String type

    SimpleField(Field parent, String type, String name) {
        super(parent, name)
        this.type = type
    }

    @Override
    String getJavaType() {
        getJavaType(type)
    }

    @Override
    void generateDeclaration(File file, String indent) {
        file << "${indent}public $javaType $name;\n"
    }

    @Override
    void generateLocalDeclaration(File file, String indent) {
        // Only count fields require local declaration
    }

    @Override
    void generateRead(File file, String indent, String name) {
        file << indent + name + ' = ' + [
                int           : 'in.readInt()',
                varint        : 'in.readVarInt()',
                string        : 'in.readString()',
                short         : 'in.readShort()',
                ushort        : 'in.readUnsignedShort()',
                long          : 'in.readLong()',
                byte          : 'in.readByte()',
                ubyte         : 'in.readUnsignedByte()',
                float         : 'in.readFloat()',
                double        : 'in.readDouble()',
                bool          : 'in.readBoolean()',
                uuid          : 'in.readUUID()',
                restBuffer    : "in.readBytes(in.available())",
                position      : 'Position.read(in)',
                slot          : 'ItemStack.read(in)',
                entityMetadata: 'EntityMetadata.read(in)',
        ][type] + ";\n"
    }

    @Override
    void generateWrite(File file, String indent, String name) {
        file << indent + [
                int           : "out.writeInt($name)",
                varint        : "out.writeVarInt($name)",
                string        : "out.writeString($name)",
                short         : "out.writeShort($name)",
                ushort        : "out.writeShort($name)",
                long          : "out.writeLong($name)",
                byte          : "out.writeByte($name)",
                ubyte         : "out.writeByte($name)",
                float         : "out.writeFloat($name)",
                double        : "out.writeDouble($name)",
                bool          : "out.writeBoolean($name)",
                uuid          : "out.writeUUID($name)",
                restBuffer    : "out.writeBytes($name)",
                position      : "${name}.write(out)",
                slot          : "ItemStack.write(${name}, out)",
                entityMetadata: "${name}.write(out)",
                container     : "${name}.write(out)",
        ][type] + ";\n"
    }

    static def getJavaType(type) {
        [
                int: 'int',
                varint: 'int',
                string: 'String',
                short: 'short',
                ushort: 'int',
                long: 'long',
                byte: 'byte',
                ubyte: 'int',
                float: 'float',
                double: 'double',
                bool: 'boolean',
                uuid: 'UUID',
                restBuffer: 'byte[]',
                position: 'Position',
                slot: 'ItemStack',
                entityMetadata: 'EntityMetadata',
        ][type]
    }
}

@InheritConstructors
class CountField extends Field {
    interface For {
        String getCount()
    }
    For forField
    CountTransformation countTransformation;

    String getJavaType() {
        child.javaType
    }

    @Override
    void generateDeclaration(File file, String indent) {
        // Count field is local only
    }

    @Override
    void generateLocalDeclaration(File file, String indent) {
        file << "${indent}int $name = 0;\n"
    }

    @Override
    void generateRead(File file, String indent, String name) {
        child.generateRead(file, indent, name)
    }

    @Override
    void generateWrite(File file, String indent, String name) {
        def transformed = countTransformation.transform(file, indent, forField.count)
        file << "${indent}int $name = $transformed;\n"
        child.generateWrite(file, indent, name)
    }
}

class BufferField extends Field implements CountField.For {
    Field count
    CountTransformation countTransformation

    BufferField(Field parent, String name, Field count, CountTransformation countTransformation) {
        super(parent, name)
        this.count = count
        this.countTransformation = countTransformation

        while (!(count instanceof CountField)) {
            count = count.child
            if (count == null) return
        }
        count.forField = this
        count.countTransformation = countTransformation
    }

    @Override
    void generateDeclaration(File file, String indent) {
        file << "${indent}public byte[] $name;\n"
    }

    @Override
    void generateLocalDeclaration(File file, String indent) {
        // None
    }

    @Override
    void generateRead(File file, String indent, String name) {
        file << indent + "$name = in.readBytes($count.name);\n"
    }

    @Override
    void generateWrite(File file, String indent, String name) {
        file << indent + "out.writeBytes($name);\n"
    }

    @Override
    String getCount() {
        name + '.length'
    }
}

@InheritConstructors
class ConditionField extends Field {
    String condition

    ConditionField(Field parent, String name, String condition) {
        super(parent, name)
        this.condition = condition
    }

    @Override
    void generateDeclaration(File file, String indent) {
        child.generateDeclaration(file, indent)
    }

    @Override
    void generateLocalDeclaration(File file, String indent) {
        child.generateLocalDeclaration(file, indent)
    }

    @Override
    void generateRead(File file, String indent, String name) {
        file << "${indent}if ($condition) {\n"
        child.generateRead(file, indent + '\t', name)
        file << indent + '}\n'
    }

    @Override
    void generateWrite(File file, String indent, String name) {
        file << "${indent}if ($condition) {\n"
        child.generateWrite(file, indent + '\t', name)
        file << indent + '}\n'
    }
}

@InheritConstructors
class ArrayField extends Field implements CountField.For {
    Field count
    CountTransformation countTransformation

    ArrayField(Field parent, String name, Field count, CountTransformation countTransformation) {
        super(parent, name)
        this.count = count
        this.countTransformation = countTransformation

        while (!(count instanceof CountField)) {
            count = count.child
            if (count == null) return
        }
        count.forField = this
        count.countTransformation = countTransformation
    }

    @Override
    String getJavaType() {
        "$child.javaType[]"
    }

    @Override
    void generateDeclaration(File file, String indent) {
        file << "${indent}public $javaType $name;\n"
    }

    @Override
    void generateLocalDeclaration(File file, String indent) {
        child.generateLocalDeclaration(file, indent)
    }

    @Override
    void generateRead(File file, String indent, String name) {
        def i = 'i_' + name
        file << indent + "int max_$i = ${countTransformation.transform(file, indent, count.name)};\n"
        file << indent + "$name = new $child.javaType[max_$i];\n"
        file << indent + "for (int $i = 0; $i < max_$i; $i++) {\n"
        child.generateRead(file, indent + '\t', "$name[$i]")
        file << indent + '}\n'
    }

    @Override
    void generateWrite(File file, String indent, String name) {
        file << indent + "for ($child.javaType $name : this.$name) {\n"
        child.generateWrite(file, indent + '\t', name)
        file << indent + '}\n'
    }

    @Override
    String getCount() {
        name + '.length'
    }
}

@InheritConstructors
class ContainerField extends Field {

    def root
    def containers = new HashSet()
    def referenced = new HashSet()
    def fields

    ContainerField(Field parent, String name, root) {
        super(parent, name)
        this.root = root
    }

    @Override
    String getJavaType() {
        def name = name
        def parent = parent
        while (parent != null) {
            if (parent instanceof ArrayField) {
                if (name.endsWith('s')) {
                    if (name.endsWith('ies')) {
                        name = name.substring(0, name.length() - 3) + 'y'
                    } else {
                        name = name.substring(0, name.length() - 1)
                    }
                } else {
                    name = name + 'Element'
                }
            }
            parent = parent.parent
        }
        return name.capitalize()
    }

    @Override
    void generateDeclaration(File file, String indent) {
        file << "$indent$javaType $name;"
    }

    @Override
    void generateLocalDeclaration(File file, String indent) {
        // None
    }

    @Override
    void generateRead(File file, String indent, String name) {
        def args = referenced.collect { ", $it.name" }.join('')
        file << "$indent${name} = new $javaType();\n"
        file << "$indent${name}.read(in$args);\n"
    }

    @Override
    void generateWrite(File file, String indent, String name) {
        def args = referenced.collect { ", $it.name" }.join('')
        file << "$indent${name}.write(out$args);\n"
    }

    void parseClass(outerFields) {
        fields = Parser.parseFields(root, outerFields, referenced, null, containers)
        referenced.retainAll(outerFields.values())
        containers.each {
            it.parseClass(fields + outerFields)
            it.referenced.each {
                if (outerFields.containsValue(it)) {
                    referenced.add(it)
                }
            }
        }
    }

    void generateClass(File file, String indent) {
        file << indent + "public static class $javaType {\n"
        Parser.generateBody(file, indent + '\t', fields, referenced, containers)
        file << "$indent}\n"
    }
}

interface CountTransformation {
    String transform(File file, String intend, String from)
}

class NoCountTransformation implements CountTransformation {
    @Override
    String transform(File file, String intend, String from) {
        from
    }
}

class SwitchTransformation implements CountTransformation {
    int defaultValue
    Map<Object, Integer> map

    SwitchTransformation(int defaultValue, Map<Object, Integer> map) {
        this.defaultValue = defaultValue
        this.map = map
    }

    @Override
    String transform(File file, String intend, String from) {
        file << intend + "int s_$from = $defaultValue;\n"
        file << intend + "switch ($from) {\n"
        map.each {
            file << "$intend\tcase $it.key: s_$from = $it.value; break;\n"
        }
        file << intend + '}\n'
        return "s_$from"
    }
}

def generatePacketHeader(f, pckg, name) {
    f << """// This file was generated automatically. Do not edit.
package $pckg;

import de.johni0702.mc.protocolgen.Packet;
import de.johni0702.mc.protocolgen.types.*;
import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;

import java.io.IOException;
import java.util.UUID;

public class $name implements Packet {
"""
}

def generatePacketFooter(f) {
    f << "}\n"
}
