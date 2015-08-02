package de.johni0702.mc.protocolgen;

import de.johni0702.mc.protocolgen.test.PacketTestBool;
import de.johni0702.mc.protocolgen.test.PacketTestByte;
import de.johni0702.mc.protocolgen.test.PacketTestDouble;
import de.johni0702.mc.protocolgen.test.PacketTestEntityMetadata;
import de.johni0702.mc.protocolgen.test.PacketTestFloat;
import de.johni0702.mc.protocolgen.test.PacketTestInt;
import de.johni0702.mc.protocolgen.test.PacketTestLong;
import de.johni0702.mc.protocolgen.test.PacketTestPosition;
import de.johni0702.mc.protocolgen.test.PacketTestRestBuffer;
import de.johni0702.mc.protocolgen.test.PacketTestShort;
import de.johni0702.mc.protocolgen.test.PacketTestSlot;
import de.johni0702.mc.protocolgen.test.PacketTestString;
import de.johni0702.mc.protocolgen.test.PacketTestUbyte;
import de.johni0702.mc.protocolgen.test.PacketTestUshort;
import de.johni0702.mc.protocolgen.test.PacketTestUuid;
import de.johni0702.mc.protocolgen.test.PacketTestVarint;
import de.johni0702.mc.protocolgen.types.EntityMetadata;
import de.johni0702.mc.protocolgen.types.ItemStack;
import de.johni0702.mc.protocolgen.types.Position;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class TestSimpleFields extends PacketReadWriteTest {
    private static final UUID theUuid = new UUID(0x0102030405060708L, 0x090a0b0c0d0e0f10L);

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> params() {
        return Arrays.asList(new Object[][] {
                {PacketTestInt.class, new byte[]{1, 2, 3, 4}, 0x01020304},
                {PacketTestVarint.class, new byte[]{(byte) 0b10000100, 0b10}, 0b100000100},
                {PacketTestString.class, new byte[]{4, 'T', 'e', 's', 't'}, "Test"},
                {PacketTestShort.class, new byte[]{1, 2}, (short) 0x0102},
                {PacketTestUshort.class, new byte[]{-1, 2}, 0xFF02},
                {PacketTestLong.class, new byte[]{1, 2, 3, 4, 5, 6, 7, 8}, 0x0102030405060708L},
                {PacketTestByte.class, new byte[]{-1}, (byte) -1},
                {PacketTestUbyte.class, new byte[]{-1}, 0xff},
                {PacketTestFloat.class, new byte[]{0, 0, 0, 0}, 0F}, // TODO Replace with proper test data
                {PacketTestDouble.class, new byte[]{0, 0, 0, 0, 0, 0, 0, 0}, 0D}, // TODO Replace with proper test data
                {PacketTestBool.class, new byte[]{0}, false},
                {PacketTestUuid.class, new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16}, theUuid},
                {PacketTestPosition.class, new byte[]{0, 0, 0, 0, 0, 0, 0, 0}, new Position(0, 0, 0)}, // TODO Replace with proper test data
                {PacketTestSlot.class, new byte[]{1, 2, 3, 4, 5, 0}, new ItemStack(0x0102, 0x03, 0x0405)},
                {PacketTestEntityMetadata.class, new byte[]{127}, new EntityMetadata()},
                {PacketTestRestBuffer.class, new byte[]{1, 2, 3, 4, 5, 6, 7}, new byte[]{1, 2, 3, 4, 5, 6, 7}},
        });
    }

    private final Packet packet;
    private final Field testField;
    private final byte[] bytes;
    private final Object value;

    public TestSimpleFields(Class<Packet> packet, byte[] bytes, Object value) throws IllegalAccessException, InstantiationException, NoSuchFieldException {
        this.packet = packet.newInstance();
        this.testField = packet.getField("test");
        this.bytes = bytes;
        this.value = value;
    }

    @Test
    public void testRead() throws IOException, IllegalAccessException {
        read(packet, bytes);

        if (value instanceof byte[]) {
            assertArrayEquals((byte[]) value, (byte[]) testField.get(packet));
        } else {
            assertEquals(value, testField.get(packet));
        }
    }

    @Test
    public void testWrite() throws IOException, IllegalAccessException {
        testField.set(packet, value);

        write(packet, bytes);
    }
}