package de.johni0702.mc.protocolgen;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.spacehq.packetlib.tcp.io.ByteBufNetInput;
import org.spacehq.packetlib.tcp.io.ByteBufNetOutput;

import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;

public class PacketReadWriteTest {

    protected byte[] bytes(int...bytes) {
        byte[] data = new byte[bytes.length];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) bytes[i];
        }
        return data;
    }

    protected void read(Packet packet, int...bytes) throws IOException {
        read(packet, bytes(bytes));
    }

    protected void read(Packet packet, byte...bytes) throws IOException {
        packet.read(new ByteBufNetInput(Unpooled.wrappedBuffer(bytes)));
    }

    protected void write(Packet packet, int...expected) throws IOException {
        write(packet, bytes(expected));
    }

    protected void write(Packet packet, byte...expected) throws IOException {
        ByteBuf buf = Unpooled.buffer();
        try {
            packet.write(new ByteBufNetOutput(buf));

            byte[] data = new byte[buf.readableBytes()];
            buf.readBytes(data);
            assertArrayEquals(expected, data);
        } finally {
            buf.release();
        }
    }
}
