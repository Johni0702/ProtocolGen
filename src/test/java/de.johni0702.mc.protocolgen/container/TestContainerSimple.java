package de.johni0702.mc.protocolgen.container;

import de.johni0702.mc.protocolgen.PacketReadWriteTest;
import de.johni0702.mc.protocolgen.test.PacketTestContainerSimple;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestContainerSimple extends PacketReadWriteTest {
    private PacketTestContainerSimple packet;

    @Before
    public void init() {
        packet = new PacketTestContainerSimple();
    }

    @Test
    public void testRead() throws IOException {
        read(packet, 1);
        assertNotNull("Container object is null.", packet.content);
        assertEquals(1, packet.content.inner);
    }

    @Test
    public void testWrite() throws IOException {
        packet.content = new PacketTestContainerSimple.Content();
        packet.content.inner = 1;
        write(packet, 1);
    }
}
