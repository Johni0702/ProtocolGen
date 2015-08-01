Reads the json protocol info (currently from [here](https://github.com/rom1504/node-minecraft-protocol/blob/snapshot-1.9/protocol/protocol.json) and turns it into java source files.

To convert json to Java run `./gradlew genPacketSource`.
To compile the generated files into a jar file run `./gradlew build`.

Uses [PacketLib](https://github.com/Steveice10/PacketLib) for NetInput/Output Streams 
and [OpenNBT](https://github.com/Steveice10/OpenNBT) for parsing of ItemStack NBT data.