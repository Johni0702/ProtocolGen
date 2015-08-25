package de.johni0702.mc.protocolgen.condition;

import de.johni0702.mc.protocolgen.PacketReadWriteTest;
import de.johni0702.mc.protocolgen.test.PacketTestSwitchRange;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class TestSwitchRange extends PacketReadWriteTest {
    private PacketTestSwitchRange packet;

    @Before
    public void init() {
        packet = new PacketTestSwitchRange();
    }

    @Test
    public void testReadInactive() throws IOException {
        read(packet, 1, 2);
        assertEquals(1, packet.before);
        assertEquals(0, packet.inner);
        assertEquals(2, packet.after);

        read(packet, 5, 2);
        assertEquals(5, packet.before);
        assertEquals(0, packet.inner);
        assertEquals(2, packet.after);
    }

    @Test
    public void testReadActive() throws IOException {
        read(packet, 2, 3, 4);
        assertEquals(2, packet.before);
        assertEquals(3, packet.inner);
        assertEquals(4, packet.after);

        read(packet, 3, 4, 5);
        assertEquals(3, packet.before);
        assertEquals(4, packet.inner);
        assertEquals(5, packet.after);

        read(packet, 4, 5, 6);
        assertEquals(4, packet.before);
        assertEquals(5, packet.inner);
        assertEquals(6, packet.after);
    }

    @Test
    public void testWriteInactive() throws IOException {
        packet.before = 1;
        packet.after = 2;
        write(packet, 1, 2);

        packet.before = 5;
        packet.after = 2;
        write(packet, 5, 2);
    }

    @Test
    public void testWriteActive() throws IOException {
        packet.before = 2;
        packet.inner = 3;
        packet.after = 4;
        write(packet, 2, 3, 4);

        packet.before = 3;
        packet.inner = 4;
        packet.after = 5;
        write(packet, 3, 4, 5);

        packet.before = 4;
        packet.inner = 5;
        packet.after = 6;
        write(packet, 4, 5, 6);
    }
}
