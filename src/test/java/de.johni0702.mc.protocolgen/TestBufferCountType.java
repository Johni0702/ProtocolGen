package de.johni0702.mc.protocolgen;

import de.johni0702.mc.protocolgen.test.PacketTestBufferCountType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertArrayEquals;

@RunWith(Parameterized.class)
public class TestBufferCountType extends PacketReadWriteTest {
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> params() {
        return Arrays.asList(new Object[][] {{0}, {1}, {2}, {3}, {5}});
    }

    private final byte[] bytes;
    private final byte[] buffer;
    private PacketTestBufferCountType packet;

    public TestBufferCountType(int length) {
        buffer = new byte[length];
        bytes = new byte[1 + length];
        bytes[0] = (byte) length;
        for (int i = 0; i < length; i++) {
            buffer[i] = (byte) i;
            bytes[1 + i] = (byte) i;
        }
    }

    @Before
    public void init() {
        packet = new PacketTestBufferCountType();
    }

    @Test
    public void testRead() throws IOException {
        read(packet, bytes);
        assertArrayEquals(buffer, packet.inner);
    }

    @Test
    public void testWrite() throws IOException {
        packet.inner = buffer;
        write(packet, bytes);
    }
}
