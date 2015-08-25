package de.johni0702.mc.protocolgen;

import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.util.UUID;

/**
 * Provides utility methods for reading and writing of additional data types.
 */
public class NetUtils {
    public static int readVarInt(ByteBuf in) throws IOException {
        int result = 0;
        int bits = 0;
        int b;
        do {
            result |= ((b = in.readByte()) & 0x7F) << bits;
            if ((bits += 7) > 35) {
                throw new IOException("VarInt longer than 5 bytes");
            }
        } while ((b & 0x80) == 0x80);
        return result;
    }

    public static byte[] readBytes(ByteBuf in, int length) throws IOException {
        byte[] bytes = new byte[length];
        in.readBytes(bytes);
        return bytes;
    }

    public static String readString(ByteBuf in) throws IOException {
        return new String(readBytes(in, readVarInt(in)), "UTF-8");
    }

    public static UUID readUUID(ByteBuf in) throws IOException {
        return new UUID(in.readLong(), in.readLong());
    }

    public static void writeVarInt(ByteBuf out, int i) throws IOException {
        while ((i & ~0x7F) != 0) {
            out.writeByte((i & 0x7F) | 0x80);
            i >>>= 7;
        }
        out.writeByte(i);
    }

    public static void writeString(ByteBuf out, String string) throws IOException {
        byte[] bytes = string.getBytes("UTF-8");
        writeVarInt(out, bytes.length);
        out.writeBytes(bytes);
    }

    public static void writeUUID(ByteBuf out, UUID uuid) throws IOException {
        out.writeLong(uuid.getMostSignificantBits());
        out.writeLong(uuid.getLeastSignificantBits());
    }
}
