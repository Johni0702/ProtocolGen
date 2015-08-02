package de.johni0702.mc.protocolgen.container;

import de.johni0702.mc.protocolgen.PacketReadWriteTest;
import de.johni0702.mc.protocolgen.test.PacketTestContainerOuterCondition;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestContainerOuterCount extends PacketReadWriteTest {
    private PacketTestContainerOuterCondition packet;

    @Before
    public void init() {
        packet = new PacketTestContainerOuterCondition();
    }

    @Test
    public void testRead() throws IOException {
        read(packet, 2);
        assertEquals(2, packet.outer);
        assertNotNull("Container object is null.", packet.content);
        assertEquals(0, packet.content.inner);
        read(packet, 1, 2);
        assertEquals(1, packet.outer);
        assertNotNull("Container object is null.", packet.content);
        assertEquals(2, packet.content.inner);
    }

    @Test
    public void testWrite() throws IOException {
        packet.content = new PacketTestContainerOuterCondition.Content();
        packet.outer = 2;
        write(packet, 2);
        packet.outer = 1;
        packet.content.inner = 2;
        write(packet, 1, 2);
    }
}
