package de.johni0702.mc.protocolgen.container;

import de.johni0702.mc.protocolgen.PacketReadWriteTest;
import de.johni0702.mc.protocolgen.test.PacketTestContainerMulti;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class TestContainerMulti extends PacketReadWriteTest {
    private PacketTestContainerMulti packet;

    @Before
    public void init() {
        packet = new PacketTestContainerMulti();
    }

    @Test
    public void testRead() throws IOException {
        read(packet, 1);
        assertNotNull("Container object is null.", packet.content);
        assertNotNull("Inner container object is null.", packet.content.innerContent);
        assertEquals(1, packet.content.innerContent.inner);
    }

    @Test
    public void testWrite() throws IOException {
        packet.content = new PacketTestContainerMulti.Content();
        packet.content.innerContent = new PacketTestContainerMulti.Content.InnerContent();
        packet.content.innerContent.inner = 1;
        write(packet, 1);
    }
}
