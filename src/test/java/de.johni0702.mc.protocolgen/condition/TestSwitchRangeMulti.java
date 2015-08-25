package de.johni0702.mc.protocolgen.condition;

import de.johni0702.mc.protocolgen.PacketReadWriteTest;
import de.johni0702.mc.protocolgen.test.PacketTestSwitchRangeMulti;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class TestSwitchRangeMulti extends PacketReadWriteTest {
    private PacketTestSwitchRangeMulti packet;

    @Before
    public void init() {
        packet = new PacketTestSwitchRangeMulti();
    }

    @Test
    public void testReadInactive() throws IOException {
        read(packet, 0, 2);
        assertEquals(0, packet.before);
        assertEquals(0, packet.inner);
        assertEquals(2, packet.after);

        read(packet, 3, 2);
        assertEquals(3, packet.before);
        assertEquals(0, packet.inner);
        assertEquals(2, packet.after);

        read(packet, 6, 2);
        assertEquals(6, packet.before);
        assertEquals(0, packet.inner);
        assertEquals(2, packet.after);
    }

    @Test
    public void testReadActive() throws IOException {
        read(packet, 1, 2, 3);
        assertEquals(1, packet.before);
        assertEquals(2, packet.inner);
        assertEquals(3, packet.after);

        read(packet, 2, 3, 4);
        assertEquals(2, packet.before);
        assertEquals(3, packet.inner);
        assertEquals(4, packet.after);

        read(packet, 4, 5, 6);
        assertEquals(4, packet.before);
        assertEquals(5, packet.inner);
        assertEquals(6, packet.after);

        read(packet, 5, 6, 7);
        assertEquals(5, packet.before);
        assertEquals(6, packet.inner);
        assertEquals(7, packet.after);
    }

    @Test
    public void testWriteInactive() throws IOException {
        packet.before = 0;
        packet.after = 2;
        write(packet, 0, 2);

        packet.before = 3;
        packet.after = 2;
        write(packet, 3, 2);

        packet.before = 6;
        packet.after = 2;
        write(packet, 6, 2);
    }

    @Test
    public void testWriteActive() throws IOException {
        packet.before = 1;
        packet.inner = 2;
        packet.after = 3;
        write(packet, 1, 2, 3);

        packet.before = 2;
        packet.inner = 3;
        packet.after = 4;
        write(packet, 2, 3, 4);

        packet.before = 4;
        packet.inner = 5;
        packet.after = 6;
        write(packet, 4, 5, 6);

        packet.before = 5;
        packet.inner = 6;
        packet.after = 7;
        write(packet, 5, 6, 7);
    }
}
