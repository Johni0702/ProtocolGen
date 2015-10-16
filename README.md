Reads the json protocol info (currently from [here](https://github.com/PrismarineJS/minecraft-data/blob/master/data/1.8/protocol.json)) and turns it into java source files.

To convert json to Java run `./gradlew genPacketSource`.
To compile the generated files into a jar file run `./gradlew build`.

Uses [OpenNBT](https://github.com/Steveice10/OpenNBT) for parsing of ItemStack NBT data.
