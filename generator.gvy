import groovy.json.JsonSlurper
import groovy.transform.InheritConstructors

task genPacketSources() {
    def protocolFile = file('src/gen/resources/protocol.json')
    def outputFolder = file('src/gen/java')
    inputs.file protocolFile
    outputs.dir outputFolder
    doLast {
        def jsonSlurper = new JsonSlurper()
        def jsonString = protocolFile.text
        // ugly hack to prevent uppercase field names
        jsonString = jsonString.replace('"UUID"', '"uuid"')
        def root = jsonSlurper.parseText(jsonString)

        root.each {
            def pckg = 'de.johni0702.mc.protocolgen.' + it.key
            def name = it.key.capitalize()
            generateProtocol(outputFolder, 'Server' + name, pckg + '.server', it.value.toServer)
            generateProtocol(outputFolder, 'Client' + name, pckg + '.client', it.value.toClient)
        }
    }
}

task genPacketTestSources() {
    def protocolFile = file('src/test/resources/test_protocol.json')
    def outputFolder = file('src/test/gen')
    inputs.file protocolFile
    outputs.dir outputFolder
    doLast {
        def jsonSlurper = new JsonSlurper()
        def jsonString = protocolFile.text
        // ugly hack to prevent uppercase field names
        jsonString = jsonString.replace('"UUID"', '"uuid"')
        def root = jsonSlurper.parseText(jsonString)
        generateProtocol(outputFolder, 'Test', 'de.johni0702.mc.protocolgen.test', root)
    }
}

def generateProtocol(sourceFolder, protocolName, pckg, root) {
    def folder = new File(sourceFolder, pckg.replace('.', '/'))
    if (!folder.exists()) folder.mkdirs()
    protocolName = 'Protocol' + protocolName
    def f = new File(folder, protocolName + '.java')
    if (f.exists()) f.delete()

    f << """// This file was generated automatically. Do not edit.
package $pckg;

import de.johni0702.mc.protocolgen.Packet;

import java.util.HashMap;

public class $protocolName extends HashMap<Integer, Class<? extends Packet>> {

\tpublic $protocolName() {
"""

    root.sort{ it.value.id }.each {
        def name = 'Packet' + it.key.capitalize().replaceAll(/_\w/){ it[1].toUpperCase() }
        generatePacket(folder, pckg, name, it.value)
        f << "\t\tput($it.value.id, ${name}.class);\n"
    }

    f << '\t}\n}\n'
}

def generatePacket(folder, pckg, name, root) {
    println "Generating $pckg.$name"
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
            def field = parseFieldWithName(it, parent, fields + parentFields, referenced, containers)
            referencedFields.addAll(referenced)
            fields.put(it.name, field)
        }
        return fields
    }

    static Field parseFieldWithName(it, Field parent, Map<String, Field> fields, Set<Field> referenced, Set<ContainerField> containers) {
        parseField(it.type, it.name, parent, fields, referenced, containers)
    }

    static Field parseField(type, name, Field parent, Map<String, Field> fields, Set<Field> referenced, Set<ContainerField> containers) {
        def typeArgs = null
        if (!(type instanceof String)) {
            typeArgs = type[1]
            type = type[0]
        }
        switch (type) {
            case 'count':
                def field = new CountField(parent, name)
                field.child = parseFieldWithName(typeArgs, field, fields, referenced, containers)
                return field
            case 'buffer':
                if (typeArgs.countType == null) {
                    Field count
                    CountTransformation countTransformation
                    (count, countTransformation) = parseCount(typeArgs.count, fields)
                    referenced.add(count)
                    return new BufferField(parent, name, count, countTransformation)
                } else {
                    return new CountedBufferField(parent, name, Type.fromJson(typeArgs.countType))
                }
            case 'switch':
                def compareTo = typeArgs.compareTo;
                if (compareTo.startsWith('this.')) {
                    compareTo = compareTo.substring('this.'.length())
                }
                compareTo = fields.get(compareTo)
                referenced.add(compareTo)
                def voids = []
                Field contentField = null
                SwitchField field = new SwitchField(parent, name, compareTo)
                typeArgs.fields.each {
                    def f = parseField(it.value, null, field, fields, referenced, containers)
                    if (f instanceof VoidField) {
                        voids << f
                    } else {
                        contentField = f
                    }
                    field.cases.put(it.key, f)
                }
                if (typeArgs.default != null) {
                    field.defaultCase = parseField(typeArgs.default, null, field, fields, referenced, containers)
                    if (field.defaultCase instanceof VoidField) {
                        voids << field.defaultCase
                    } else {
                        contentField = field.defaultCase
                    }
                }
                voids.each {
                    it.type = contentField.type
                }
                field.child = contentField
                return field
            case 'array':
                def field
                if (typeArgs.countType == null) {
                    Field count
                    CountTransformation countTransformation
                    (count, countTransformation) = parseCount(typeArgs.count, fields)
                    referenced.add(count)
                    field = new ArrayField(parent, name, count, countTransformation)
                    field.child = parseFieldWithName(typeArgs, field, fields, referenced, containers)
                } else {
                    field = new CountedArrayField(parent, name, Type.fromJson(typeArgs.countType))
                    field.counted.child = parseFieldWithName(typeArgs, field, fields, referenced, containers)
                }
                return field
            case 'container':
                def field = new ContainerField(parent, name, typeArgs)
                containers.add(field)
                return field
            case 'void':
                return new VoidField(parent, name)
            default:
                return new SimpleField(parent, Type.fromJson(type), name)
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
        def args = referenced.collect { ", $it.type.java $it.name" }.join('')
        f << indent + (indent.length() == 1 ? 'public' : 'private') + " void read(ByteBuf in$args) throws IOException {\n"
        fields.values().each { it.generateLocalDeclaration(f, indent + '\t') }
        fields.values().each { it.generateRead(f, indent + '\t', it.name) }
        f << indent + '}\n'
        f << '\n'

        // Write
        args = referenced.collect { ", $it.type.java $it.name" }.join('')
        f << indent + (indent.length() == 1 ? 'public' : 'private') + " void write(ByteBuf out$args) throws IOException {\n"
        fields.values().each { it.generateWrite(f, indent + '\t', it.name) }
        f << indent + '}\n'
        f << '\n'

        // Containers / Inner classes
        containers.each { it.generateClass(f, indent) }
    }
}

class Type {
    static List<Type> values = []

    final String java
    final String json
    final String read
    final Closure<String> write
    final String defaultValue

    Type(String java, String json, String read, Closure<String> write, String defaultValue) {
        this.java = java
        this.json = json
        this.read = read
        this.write = write
        this.defaultValue = defaultValue
    }

    public static Type fromJson(String jsonType) {
        values.find {it.json == jsonType}
    }

    static {
        values << new Type('int', 'int', 'in.readInt()', {"out.writeInt($it)"}, "0");
        values << new Type('int', 'varint', 'NetUtils.readVarInt(in)', {"NetUtils.writeVarInt(out, $it)"}, "0");
        values << new Type('String', 'string', 'NetUtils.readString(in)', {"NetUtils.writeString(out, $it)"}, "null");
        values << new Type('short', 'short', 'in.readShort()', {"out.writeShort($it)"}, "0");
        values << new Type('int', 'ushort', 'in.readUnsignedShort()', {"out.writeShort($it)"}, "0");
        values << new Type('long', 'long', 'in.readLong()', {"out.writeLong($it)"}, "0");
        values << new Type('byte', 'byte', 'in.readByte()', {"out.writeByte($it)"}, "0");
        values << new Type('int', 'ubyte', 'in.readUnsignedByte()', {"out.writeByte($it)"}, "0");
        values << new Type('float', 'float', 'in.readFloat()', {"out.writeFloat($it)"}, "0");
        values << new Type('double', 'double', 'in.readDouble()', {"out.writeDouble($it)"}, "0");
        values << new Type('boolean', 'bool', 'in.readBoolean()', {"out.writeBoolean($it)"}, "false");
        values << new Type('UUID', 'uuid', 'NetUtils.readUUID(in)', {"NetUtils.writeUUID(out, $it)"}, "null");
        values << new Type('byte[]', 'restBuffer', 'NetUtils.readBytes(in, in.readableBytes())', {"out.writeBytes($it)"}, "null");
        values << new Type('Position', 'position', 'Position.read(in)', {"${it}.write(out)"}, "null");
        values << new Type('ItemStack', 'slot', 'ItemStack.read(in)', {"ItemStack.write($it, out)"}, "null");
        values << new Type('EntityMetadata', 'entityMetadata', 'EntityMetadata.read(in)', {"${it}.write(out)"}, "null");
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

    Type getType() {
        parent?.type
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
    Type type

    SimpleField(Field parent, Type type, String name) {
        super(parent, name)
        this.type = type
    }

    @Override
    Type getType() {
        type
    }

    @Override
    void generateDeclaration(File file, String indent) {
        file << "${indent}public $type.java $name;\n"
    }

    @Override
    void generateLocalDeclaration(File file, String indent) {
        // Only count fields require local declaration
    }

    @Override
    void generateRead(File file, String indent, String name) {
        file << indent + name + ' = ' + type.read + ";\n"
    }

    @Override
    void generateWrite(File file, String indent, String name) {
        file << indent + type.write(name) + ";\n"
    }
}

class VoidField extends Field {
    Type type

    VoidField(Field parent, String name) {
        super(parent, name)
    }

    @Override
    Type getType() {
        type
    }

    @Override
    void generateDeclaration(File file, String indent) {
        file << "${indent}public $type.java $name;\n"
    }

    @Override
    void generateLocalDeclaration(File file, String indent) {
    }

    @Override
    void generateRead(File file, String indent, String name) {
        file << indent + name + ' = ' + type.defaultValue + ";\n"
    }

    @Override
    void generateWrite(File file, String indent, String name) {
    }
}

@InheritConstructors
class CountField extends Field {
    interface For {
        String getCount()
    }
    For forField
    CountTransformation countTransformation;

    @Override
    Type getType() {
        child.type
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

class CountedField extends Field {
    Field count
    Field counted

    CountedField(Field parent, String name, Type countType) {
        super(parent, name)
        count = new CountField(this, "${->this.name}\$count")
        count.child = new SimpleField(count, countType, null)
    }

    @Override
    Type getType() {
        counted.type
    }

    @Override
    void generateDeclaration(File file, String indent) {
        counted.generateDeclaration(file, indent)
    }

    @Override
    void generateLocalDeclaration(File file, String indent) {
        count.generateLocalDeclaration(file, indent)
    }

    @Override
    void generateRead(File file, String indent, String name) {
        count.generateRead(file, indent, name + '$count')
        counted.generateRead(file, indent, name)
    }

    @Override
    void generateWrite(File file, String indent, String name) {
        count.generateWrite(file, indent, name + '$count')
        counted.generateWrite(file, indent, name)
    }
}

class CountedBufferField extends CountedField {
    CountedBufferField(Field parent, String name, Type countType) {
        super(parent, name, countType)
        counted = new BufferField(this, name, count, new NoCountTransformation())
    }
}

class CountedArrayField extends CountedField {
    CountedArrayField(Field parent, String name, Type countType) {
        super(parent, name, countType)
        counted = new ArrayField(this, name, count, new NoCountTransformation())
    }
}

class BufferField extends Field implements CountField.For {
    static Type type = new Type('byte[]', 'buffer', null, null, "null")
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
    Type getType() {
        type
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
        file << indent + "$name = NetUtils.readBytes(in, $count.name);\n"
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
class SwitchField extends Field {
    Field compareTo
    Map<String, Field> cases = [:]
    Field defaultCase

    SwitchField(Field parent, String name, Field compareTo) {
        super(parent, name)
        this.compareTo = compareTo

    }

    @Override
    Type getType() {
        child.type
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
        if (compareTo.type.java == 'boolean') {
            def onTrue = cases.containsKey("true") && !(cases.get("true") instanceof VoidField) ? cases.get("true") : defaultCase
            def onFalse = cases.containsKey("false") && !(cases.get("false") instanceof VoidField) ? cases.get("false") : defaultCase
            file << "${indent}if ($compareTo.name) {\n"
            onTrue.generateRead(file, indent + '\t', name)
            file << indent + '} else {\n'
            onFalse.generateRead(file, indent + '\t', name)
            file << indent + '}\n'
        } else {
            file << "${indent}switch ($compareTo.name) {\n"
            cases.each {
                if (compareTo.type.java == 'String') {
                    file << "$indent\tcase \"$it.key\": {\n"
                } else {
                    file << "$indent\tcase $it.key: {\n"
                }
                it.value.generateRead(file, indent + '\t\t', name)
                file << "$indent\t\tbreak;\n"
                file << "$indent\t}\n"
            }
            file << "$indent\tdefault:\n"
            if (defaultCase != null) {
                defaultCase.generateRead(file, indent + '\t\t', name)
            } else {
                file << "$indent\t\tthrow new IllegalArgumentException(String.valueOf($compareTo.name));\n"
            }
            file << indent + '}\n'
        }
    }

    @Override
    void generateWrite(File file, String indent, String name) {
        if (compareTo.type.java == 'boolean') {
            def onTrue = cases.containsKey("true") && !(cases.get("true") instanceof VoidField) ? cases.get("true") : defaultCase
            def onFalse = cases.containsKey("false") && !(cases.get("false") instanceof VoidField) ? cases.get("false") : defaultCase
            file << "${indent}if ($compareTo.name) {\n"
            onTrue.generateWrite(file, indent + '\t', name)
            file << indent + '} else {\n'
            onFalse.generateWrite(file, indent + '\t', name)
            file << indent + '}\n'
        } else {
            file << "${indent}switch ($compareTo.name) {\n"
            cases.each {
                if (compareTo.type.java == 'String') {
                    file << "$indent\tcase \"$it.key\": {\n"
                } else {
                    file << "$indent\tcase $it.key: {\n"
                }
                it.value.generateWrite(file, indent + '\t\t', name)
                file << "$indent\t\tbreak;\n"
                file << "$indent\t}\n"
            }
            file << "$indent\tdefault:\n"
            if (defaultCase != null) {
                defaultCase.generateWrite(file, indent + '\t\t', name)
            } else {
                file << "$indent\t\tthrow new IllegalArgumentException(String.valueOf($compareTo.name));\n"
            }
            file << indent + '}\n'
        }
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
    Type getType() {
        new Type("$child.type.java[]", null, null, null, "null")
    }

    @Override
    void generateDeclaration(File file, String indent) {
        file << "${indent}public $type.java $name;\n"
    }

    @Override
    void generateLocalDeclaration(File file, String indent) {
        child.generateLocalDeclaration(file, indent)
    }

    @Override
    void generateRead(File file, String indent, String name) {
        def i = 'i_' + name
        file << indent + "int max_$i = ${countTransformation.transform(file, indent, count.name)};\n"
        file << indent + "$name = new $child.type.java[max_$i];\n"
        file << indent + "for (int $i = 0; $i < max_$i; $i++) {\n"
        child.generateRead(file, indent + '\t', "$name[$i]")
        file << indent + '}\n'
    }

    @Override
    void generateWrite(File file, String indent, String name) {
        file << indent + "for ($child.type.java $name : this.$name) {\n"
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
    Type getType() {
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
        return new Type(name.capitalize(), null, null, null, "null")
    }

    @Override
    void generateDeclaration(File file, String indent) {
        file << "${indent}public $type.java $name;\n"
    }

    @Override
    void generateLocalDeclaration(File file, String indent) {
        // None
    }

    @Override
    void generateRead(File file, String indent, String name) {
        def args = referenced.collect { ", $it.name" }.join('')
        file << "$indent${name} = new $type.java();\n"
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
        file << indent + "public static class $type.java {\n"
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
import de.johni0702.mc.protocolgen.NetUtils;
import de.johni0702.mc.protocolgen.types.*;
import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.util.UUID;

public class $name implements Packet {
"""
}

def generatePacketFooter(f) {
    f << "}\n"
}
