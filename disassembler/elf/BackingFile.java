package disassembler.elf;

import java.util.*;
import java.io.*;
import java.nio.*;

public class BackingFile {
    private final ByteArrayInputStream byteArray;
    private final MappedByteBuffer mappedByteBuffer;
    private final long mbbStartPosition;

    public BackingFile(ByteArrayInputStream byteArray) {
        this.byteArray = byteArray;
        this.mappedByteBuffer = null;
        this.mbbStartPosition = -1;
    }

    public BackingFile(MappedByteBuffer mappedByteBuffer) {
        this.byteArray = null;
        this.mappedByteBuffer = mappedByteBuffer;
        this.mbbStartPosition = 0;
        ((Buffer)mappedByteBuffer).position((int) mbbStartPosition);
    }

    public void seek(long offset) {
        if (byteArray != null) {
            byteArray.reset();
            if (byteArray.skip(offset) != offset) throw new Exception("seeking outside file");
        } else if (mappedByteBuffer != null) {
            ((Buffer)mappedByteBuffer).position((int)(mbbStartPosition + offset));
        }
    }

    public void skip(int bytesToSkip) {
        if (byteArray != null) {
            long skipped = byteArray.skip(bytesToSkip);
            if (skipped != bytesToSkip) {
                throw new IllegalArgumentException("Wanted to skip " + bytesToSkip + " bytes, but only able to skip " + skipped);
            }
        } else {
            ((Buffer)mappedByteBuffer).position(mappedByteBuffer.position() + bytesToSkip);
        }
    }

    short readUnsignedByte() {
        int val = -1;
        if (byteArray != null) {
            val = byteArray.read();
        } else if (mappedByteBuffer != null) {
            byte temp = mappedByteBuffer.get();
            val = temp & 0xFF;
        }

        if (val < 0) throw new Exception("Trying to read outside file");
        return (short) val;
    }

    public int read(byte[] data) {
        if (byteArray != null) {
            try {
                return byteArray.read(data);
            } catch (IOException e) {
                throw new RuntimeException("Error reading " + data.length + " bytes", e);
            }
        } else if (mappedByteBuffer != null) {
            mappedByteBuffer.get(data);
            return data.length;
        }
        throw new RuntimeException("No way to read from file or buffer");
    }
}
