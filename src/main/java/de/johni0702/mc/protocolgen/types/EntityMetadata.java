package de.johni0702.mc.protocolgen.types;

import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class EntityMetadata {
    private final Map<Integer, Object> data = new HashMap<Integer, Object>();

    public boolean hasData(int key) {
        return data.containsKey(key);
    }

    public Object getData(int key) {
        return data.get(key);
    }

    public void putData(int key, Object obj) {
        data.put(key, obj);
    }

    public static EntityMetadata read(NetInput in) throws IOException {
        EntityMetadata metadata = new EntityMetadata();
        int b;
        while ((b = in.readUnsignedByte()) != 127) {
            int type = (b & 0xE0) >> 5;
            int key = b & 0x1F;
            Object value;
            switch (type) {
                case 0:
                    value = in.readByte();
                    break;
                case 1:
                    value = in.readShort();
                    break;
                case 2:
                    value = in.readInt();
                    break;
                case 3:
                    value = in.readFloat();
                    break;
                case 4:
                    value = in.readString();
                    break;
                case 5:
                    value = ItemStack.read(in);
                    break;
                case 6:
                    value = new Position(in.readInt(), in.readInt(), in.readInt());
                    break;
                case 7:
                    value = new Rotation(in.readFloat(), in.readFloat(), in.readFloat());
                    break;
                default:
                    throw new IllegalArgumentException("Unknown metadata type: " + type);
            }
            metadata.putData(key, value);
        }
        return metadata;
    }

    public void write(NetOutput out) throws IOException {
        for (Map.Entry<Integer, Object> e : data.entrySet()) {
            int key = e.getKey();
            Object value = e.getValue();
            int type;
            if (value instanceof Byte) type = 0;
            else if (value instanceof Short) type = 1;
            else if (value instanceof Integer) type = 2;
            else if (value instanceof Float) type = 3;
            else if (value instanceof String) type = 4;
            else if (value instanceof ItemStack) type = 5;
            else if (value instanceof Position) type = 6;
            else if (value instanceof Rotation) type = 7;
            else throw new IllegalArgumentException("Unknown metadata type: " + value.getClass());

            out.writeByte(type << 5 | key & 0x1F);

            switch (type) {
                case 0:
                    out.writeByte((Byte) value);
                    break;
                case 1:
                    out.writeShort((Short) value);
                    break;
                case 2:
                    out.writeInt((Integer) value);
                    break;
                case 3:
                    out.writeFloat((Float) value);
                    break;
                case 4:
                    out.writeString((String) value);
                    break;
                case 5:
                    ItemStack.write((ItemStack) value, out);
                    break;
                case 6:
                    ((Position) value).write(out);
                    break;
                case 7:
                    Rotation rotation = (Rotation) value;
                    out.writeFloat(rotation.getPitch());
                    out.writeFloat(rotation.getYaw());
                    out.writeFloat(rotation.getRoll());
                    break;
                default:
                    throw new IllegalArgumentException("Unknown metadata type: " + type);
            }
        }
        out.writeByte(127);
    }
}
