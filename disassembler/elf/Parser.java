package disassembler.elf;

import java.util.*;
import java.io.*;
import java.nio.*;

public class Parser {
    final ELF elfFile;
    private final BackingFile backingFile;
    private long readBytes;

    Parser(ELF elfFile, BackingFile backingFile) {
        this.elfFile = elfFile;
        this.backingFile = backingFile;
    }

    public void seek(long offset) {
        readBytes = 0;
        backingFile.seek(offset);
    }

    public void skip(int bytesToSkip) {
        readBytes = 0;
        backingFile.skip(bytesToSkip);
    }

    public long getReadBytes() {
        return readBytes;
    }

    short byteSwap(short arg) {
        return (short) ((arg << 8) | ((arg >>> 8) & 0xFF));
    }

    int byteSwap(int arg) {
        return ((byteSwap((short) arg)) << 16) | (((byteSwap((short) (arg >>> 16)))) & 0xFFFF);
    }

    long byteSwap(long arg) {
        return ((((long) byteSwap((int) arg)) << 32) | (((long) byteSwap((int) (arg >>> 32))) & 0xFFFFFFFF));
    }

    short readUnsignedByte() {
        readBytes++;
        return backingFile.readUnsignedByte();
    }

    public short readShort() throws Exception {
        int ch1 = readUnsignedByte();
        int ch2 = readUnsignedByte();
        short val = (short) ((ch1 << 8) + (ch2 << 0));
        if (elfFile.encoding == ELF.DATA_LSB) val = byteSwap(val);
        return val;
    }

    public int readInt() throws Exception {
        int ch1 = readUnsignedByte();
        int ch2 = readUnsignedByte();
        int ch3 = readUnsignedByte();
        int ch4 = readUnsignedByte();
        int val = ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4));

        if (elfFile.encoding == ELF.DATA_LSB) val = byteSwap(val);
        return val;
    }

    public long readLong() {
        int ch1 = readUnsignedByte();
        int ch2 = readUnsignedByte();
        int ch3 = readUnsignedByte();
        int ch4 = readUnsignedByte();
        int val1 = ((ch1 << 24) + (ch2 << 16) + (ch3 << 8) + (ch4 << 0));
        int ch5 = readUnsignedByte();
        int ch6 = readUnsignedByte();
        int ch7 = readUnsignedByte();
        int ch8 = readUnsignedByte();
        int val2 = ((ch5 << 24) + (ch6 << 16) + (ch7 << 8) + (ch8 << 0));

        long val = ((long) (val1) << 32) + (val2 & 0xFFFFFFFFL);
        if (elfFile.encoding == ELF.DATA_LSB) val = byteSwap(val);
        return val;
    }

    public long readIntOrLong() {
        return elfFile.objectSize == ELF.CLASS_32 ? readInt() : readLong();
    }

    public long unsignedByte(int arg) {
        long val;
        if (arg >= 0) {
            val = arg;
        } else {
            val = (unsignedByte((short) (arg >>> 16)) << 16) | ((short) arg);
        }
        return val;
    }

    long virtualMemoryAddrToFileOffset(long address) {
        for (int i = 0; i < elfFile.num_ph; i++) {
            Segment ph = elfFile.getProgramHeader(i);
            if (address >= ph.virtual_address && address < (ph.virtual_address + ph.mem_size)) {
                long relativeOffset = address - ph.virtual_address;
                if (relativeOffset >= ph.file_size)
                    throw new Exception("Can not convert virtual memory address " + Long.toHexString(address) + " to file offset -" + " found segment " + ph
                            + " but address maps to memory outside file range");
                return ph.offset + relativeOffset;
            }
        }
        throw new Exception("Cannot find segment for address " + Long.toHexString(address));
    }

    public int read(byte[] data) {
        return backingFile.read(data);
    }
}
