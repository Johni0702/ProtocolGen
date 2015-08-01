package de.johni0702.mc.protocolgen;

import org.spacehq.packetlib.io.NetInput;
import org.spacehq.packetlib.io.NetOutput;

import java.io.IOException;

public interface Packet {
    void read(NetInput in) throws IOException;
    void write(NetOutput out) throws IOException;
}
