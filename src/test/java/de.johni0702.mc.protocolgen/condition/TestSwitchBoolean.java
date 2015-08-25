package de.johni0702.mc.protocolgen.condition;

import de.johni0702.mc.protocolgen.PacketReadWriteTest;
import de.johni0702.mc.protocolgen.test.PacketTestSwitchBoolean;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class TestSwitchBoolean extends PacketReadWriteTest {
    private PacketTestSwitchBoolean packet;

    @Before
    public void init() {
        packet = new PacketTestSwitchBoolean();
    }

    @Test
    public void testReadInactive() throws IOException {
        read(packet, 0, 2);
        assertEquals(false, packet.before);
        assertEquals(0, packet.inner);
        assertEquals(2, packet.after);
    }

    @Test
    public void testReadActive() throws IOException {
        read(packet, 1, 3, 4);
        assertEquals(true, packet.before);
        assertEquals(3, packet.inner);
        assertEquals(4, packet.after);
    }

    @Test
    public void testWriteInactive() throws IOException {
        packet.before = false;
        packet.after = 2;
        write(packet, 0, 2);
    }

    @Test
    public void testWriteActive() throws IOException {
        packet.before = true;
        packet.inner = 3;
        packet.after = 4;
        write(packet, 1, 3, 4);
    }
}
