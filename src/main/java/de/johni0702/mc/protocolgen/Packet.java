package de.johni0702.mc.protocolgen;

import io.netty.buffer.ByteBuf;

import java.io.IOException;

public interface Packet {
    void read(ByteBuf in) throws IOException;
    void write(ByteBuf out) throws IOException;
}
