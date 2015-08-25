package de.johni0702.mc.protocolgen.condition;

import de.johni0702.mc.protocolgen.PacketReadWriteTest;
import de.johni0702.mc.protocolgen.test.PacketTestSwitchString;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class TestSwitchString extends PacketReadWriteTest {
    private PacketTestSwitchString packet;

    @Before
    public void init() {
        packet = new PacketTestSwitchString();
    }

    @Test
    public void testReadInactive() throws IOException {
        read(packet, 7, 'I', 'n', 'v', 'a', 'l', 'i', 'd', 2);
        assertEquals("Invalid", packet.before);
        assertEquals(0, packet.inner);
        assertEquals(2, packet.after);
    }

    @Test
    public void testReadActive() throws IOException {
        read(packet, 5, 'V', 'a', 'l', 'i', 'd', 3, 4);
        assertEquals("Valid", packet.before);
        assertEquals(3, packet.inner);
        assertEquals(4, packet.after);
    }

    @Test
    public void testWriteInactive() throws IOException {
        packet.before = "Invalid";
        packet.after = 2;
        write(packet, 7, 'I', 'n', 'v', 'a', 'l', 'i', 'd', 2);
    }

    @Test
    public void testWriteActive() throws IOException {
        packet.before = "Valid";
        packet.inner = 3;
        packet.after = 4;
        write(packet, 5, 'V', 'a', 'l', 'i', 'd', 3, 4);
    }
}
