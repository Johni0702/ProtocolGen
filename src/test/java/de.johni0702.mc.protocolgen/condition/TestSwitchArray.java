package de.johni0702.mc.protocolgen.condition;

import de.johni0702.mc.protocolgen.PacketReadWriteTest;
import de.johni0702.mc.protocolgen.test.PacketTestSwitchArray;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

public class TestSwitchArray extends PacketReadWriteTest {
    private PacketTestSwitchArray packet;

    @Before
    public void init() {
        packet = new PacketTestSwitchArray();
    }

    @Test
    public void testReadInactive() throws IOException {
        read(packet, 1, 2);
        assertEquals(1, packet.before);
        assertEquals(null, packet.inner);
        assertEquals(2, packet.after);
    }

    @Test
    public void testReadActive() throws IOException {
        read(packet, 2, 4, 7, 11, 13, 17, 4);
        assertEquals(2, packet.before);
        assertArrayEquals(new byte[]{7, 11, 13, 17}, packet.inner);
        assertEquals(4, packet.after);
    }

    @Test
    public void testWriteInactive() throws IOException {
        packet.before = 1;
        packet.after = 2;
        write(packet, 1, 2);
    }

    @Test
    public void testWriteActive() throws IOException {
        packet.before = 2;
        packet.inner = new byte[]{7, 11, 13, 17};
        packet.after = 4;
        write(packet, 2, 4, 7, 11, 13, 17, 4);
    }
}
