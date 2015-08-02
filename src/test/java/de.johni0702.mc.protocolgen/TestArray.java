package de.johni0702.mc.protocolgen;

import de.johni0702.mc.protocolgen.test.PacketTestArray;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertArrayEquals;

@RunWith(Parameterized.class)
public class TestArray extends PacketReadWriteTest {
    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> params() {
        return Arrays.asList(new Object[][] {{0}, {1}, {2}, {3}, {5}});
    }

    private final byte[] bytes;
    private final short[] array;
    private PacketTestArray packet;

    public TestArray(int length) {
        array = new short[length];
        bytes = new byte[1 + length * 2];
        bytes[0] = (byte) length;
        for (int i = 0; i < length; i++) {
            array[i] = (byte) i;
            bytes[2 + i * 2] = (byte) i;
        }
    }

    @Before
    public void init() {
        packet = new PacketTestArray();
    }

    @Test
    public void testRead() throws IOException {
        read(packet, bytes);
        assertArrayEquals(array, packet.inner);
    }

    @Test
    public void testWrite() throws IOException {
        packet.inner = array;
        write(packet, bytes);
    }
}
