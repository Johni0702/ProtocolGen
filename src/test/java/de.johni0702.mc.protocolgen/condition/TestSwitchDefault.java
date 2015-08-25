package de.johni0702.mc.protocolgen.condition;

import de.johni0702.mc.protocolgen.PacketReadWriteTest;
import de.johni0702.mc.protocolgen.test.PacketTestSwitchDefault;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class TestSwitchDefault extends PacketReadWriteTest {
    private PacketTestSwitchDefault packet;

    @Before
    public void init() {
        packet = new PacketTestSwitchDefault();
    }

    @Test
    public void testReadInactive() throws IOException {
        read(packet, 2, 3);
        assertEquals(2, packet.before);
        assertEquals(0, packet.inner);
        assertEquals(3, packet.after);
    }

    @Test
    public void testReadActive() throws IOException {
        read(packet, 1, 3, 4);
        assertEquals(1, packet.before);
        assertEquals(3, packet.inner);
        assertEquals(4, packet.after);
        read(packet, 3, 4, 5);
        assertEquals(3, packet.before);
        assertEquals(4, packet.inner);
        assertEquals(5, packet.after);
    }

    @Test
    public void testWriteInactive() throws IOException {
        packet.before = 2;
        packet.after = 3;
        write(packet, 2, 3);
    }

    @Test
    public void testWriteActive() throws IOException {
        packet.before = 1;
        packet.inner = 3;
        packet.after = 4;
        write(packet, 1, 3, 4);
        packet.before = 3;
        packet.inner = 4;
        packet.after = 5;
        write(packet, 3, 4, 5);
    }
}
