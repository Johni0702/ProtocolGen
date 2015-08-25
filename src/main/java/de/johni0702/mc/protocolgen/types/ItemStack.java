package de.johni0702.mc.protocolgen.types;

import io.netty.buffer.ByteBuf;
import org.spacehq.opennbt.NBTIO;
import org.spacehq.opennbt.tag.builtin.CompoundTag;

import java.io.*;

public final class ItemStack {
    private final int id;
    private final int amount;
    private final int data;
    private final CompoundTag nbt;

    public ItemStack(int id) {
        this(id, 1);
    }

    public ItemStack(int id, int amount) {
        this(id, amount, 0);
    }

    public ItemStack(int id, int amount, int data) {
        this(id, amount, data, null);
    }

    public ItemStack(int id, int amount, int data, CompoundTag nbt) {
        this.id = id;
        this.amount = amount;
        this.data = data;
        this.nbt = nbt;
    }

    public int getId() {
        return id;
    }

    public int getAmount() {
        return amount;
    }

    public int getData() {
        return data;
    }

    public CompoundTag getNbt() {
        return nbt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ItemStack itemStack = (ItemStack) o;

        if (id != itemStack.id) return false;
        if (amount != itemStack.amount) return false;
        if (data != itemStack.data) return false;
        return !(nbt != null ? !nbt.equals(itemStack.nbt) : itemStack.nbt != null);

    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + amount;
        result = 31 * result + data;
        result = 31 * result + (nbt != null ? nbt.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "ItemStack{" +
                "id=" + id +
                ", amount=" + amount +
                ", data=" + data +
                ", nbt=" + nbt +
                '}';
    }

    public static ItemStack read(final ByteBuf in) throws IOException {
        int id = in.readShort();
        if (id == -1) {
            return null;
        } else {
            int amount = in.readByte();
            int data = in.readShort();
            CompoundTag nbt = (CompoundTag) NBTIO.readTag(new DataInputStream(new InputStream() {
                @Override
                public int read() throws IOException {
                    return in.readUnsignedByte();
                }
            }));
            return new ItemStack(id, amount, data, nbt);
        }
    }

    public static void write(ItemStack is, final ByteBuf out) throws IOException {
        if (is == null) {
            out.writeShort(-1);
        } else {
            out.writeShort(is.id);
            out.writeByte(is.amount);
            out.writeShort(is.data);
            if (is.nbt == null) {
                out.writeByte(0);
            } else {
                NBTIO.writeTag(new DataOutputStream(new OutputStream() {
                    @Override
                    public void write(int b) throws IOException {
                        out.writeByte(b);
                    }
                }), is.nbt);
            }
        }
    }
}
