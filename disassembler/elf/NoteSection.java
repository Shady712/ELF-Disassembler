package disassembler.elf;

import java.util.*;
import java.io.*;
import java.nio.*;

public class NoteSection extends Section {
    public static final int NT_GNU_ABI_TAG = 1;
    public static final int NT_GNU_HWCAP = 2;
    public static final int NT_GNU_BUILD_ID = 3;
    public static final int NT_GNU_GOLD_VERSION = 4;

    public final static class GnuAbiDescriptor {
        public static final int ELF_NOTE_OS_LINUX = 0;
        public static final int ELF_NOTE_OS_GNU = 1;
        public static final int ELF_NOTE_OS_SOLARIS2 = 2;
        public static final int ELF_NOTE_OS_FREEBSD = 3;
        public final int operatingSystem;
        public final int majorVersion;
        public final int minorVersion;
        public final int subminorVersion;
        public GnuAbiDescriptor(int operatingSystem, int majorVersion, int minorVersion, int subminorVersion) {
            this.operatingSystem = operatingSystem;
            this.majorVersion = majorVersion;
            this.minorVersion = minorVersion;
            this.subminorVersion = subminorVersion;
        }
    }

    public final int nameSize;
    public final int descriptorSize;
    public final int type;
    private String name;
    private byte[] descriptorBytes;
    private final GnuAbiDescriptor gnuAbiDescriptor;

    NoteSection(Parser parser, SectionHeader header) throws Exception {
        super(parser, header);

        parser.seek(header.section_offset);
        nameSize = parser.readInt();
        descriptorSize = parser.readInt();
        type = parser.readInt();
        byte[] nameBytes = new byte[nameSize];
        descriptorBytes = new byte[descriptorSize];
        int bytesRead = parser.read(nameBytes);
        if (bytesRead != nameSize) {
            throw new Exception("Error reading note name (read=" + bytesRead + ", expected=" + nameSize + ")");
        }
        parser.skip(bytesRead % 4);

        switch (type) {
            case NT_GNU_ABI_TAG:
                gnuAbiDescriptor = new GnuAbiDescriptor(parser.readInt(), parser.readInt(), parser.readInt(), parser.readInt());
                break;
            default:
                gnuAbiDescriptor = null;
        }

        bytesRead = parser.read(descriptorBytes);
        if (bytesRead != descriptorSize) {
            throw new Exception("Error reading note name (read=" + bytesRead + ", expected=" + descriptorSize + ")");
        }

        name = new String(nameBytes, 0, nameSize-1); // unnecessary trailing 0
    }

    String getName() {
        return name;
    }

    byte[] descriptorBytes() {
        return descriptorBytes;
    }

    public String descriptorAsString() {
        return new String(descriptorBytes);
    }

    public GnuAbiDescriptor descriptorAsGnuAbi() {
        return gnuAbiDescriptor;
    }
}
