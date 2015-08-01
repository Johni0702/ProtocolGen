package de.johni0702.mc.protocolgen.types;

import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;

import java.io.IOException;

public final class Position {
    private final int x;
    private final int y;
    private final int z;

    public Position(int x, int y, int z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Position position = (Position) o;

        if (x != position.x) return false;
        if (y != position.y) return false;
        return z == position.z;

    }

    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        result = 31 * result + z;
        return result;
    }

    @Override
    public String toString() {
        return "Position{" +
                "x=" + x +
                ", y=" + y +
                ", z=" + z +
                '}';
    }

    public static Position read(NetInput in) throws IOException {
        long val = in.readLong();
        return new Position((int) (val >> 38), (int) ((val >> 26) & 0xfff), (int) (val << 38 >> 38));
    }

    public void write(NetOutput out) throws IOException {
        out.writeLong((((long)x & 0x3FFFFFF) << 38) | (((long)y & 0xFFF) << 26) | (z & 0x3FFFFFF));
    }
}
