package de.johni0702.mc.protocolgen.condition;

import de.johni0702.mc.protocolgen.PacketReadWriteTest;
import de.johni0702.mc.protocolgen.test.PacketTestSwitchNoDefault;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class TestSwitchNoDefault extends PacketReadWriteTest {
    private PacketTestSwitchNoDefault packet;

    @Before
    public void init() {
        packet = new PacketTestSwitchNoDefault();
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadInactive() throws IOException {
        read(packet, 1, 2);
    }

    @Test
    public void testReadActive() throws IOException {
        read(packet, 2, 3, 4);
        assertEquals(2, packet.before);
        assertEquals(3, packet.inner);
        assertEquals(4, packet.after);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWriteInactive() throws IOException {
        packet.before = 1;
        packet.after = 2;
        write(packet, 1, 2);
    }

    @Test
    public void testWriteActive() throws IOException {
        packet.before = 2;
        packet.inner = 3;
        packet.after = 4;
        write(packet, 2, 3, 4);
    }
}
