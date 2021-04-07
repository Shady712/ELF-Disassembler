package disassembler.elf;

import java.util.*;
import java.io.*;
import java.nio.*;

public class StringTable extends Section {
    private final byte[] data;
    public final int numStrings;

    StringTable(Parser parser, long offset, int length, SectionHeader header) throws Exception {
        super(parser, header);
        parser.seek(offset);
        data = new byte[length];
        int bytesRead = parser.read(data);
        if (bytesRead != length)
            throw new Exception("Error reading string table (read " + bytesRead + "bytes - expected to " + "read " + data.length + "bytes)");
        int stringsCount = 0;
        for (byte datum : data) if (datum == '\0') stringsCount++;
        numStrings = stringsCount;
    }

    public String get(int index) {
        int endPtr = index;
        while (data[endPtr] != '\0')
            endPtr++;
        return new String(data, index, endPtr - index);
    }
}
