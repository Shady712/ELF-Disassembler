package disassembler.elf;

import java.util.*;
import java.io.*;
import java.nio.*;

public class Segment {
    public static final int PT_NULL = 0;
    public static final int PT_LOAD = 1;
    public static final int PT_DYNAMIC = 2;
    public static final int PT_INTERP = 3;
    public static final int PT_NOTE = 4;
    public static final int PT_SHLIB = 5;
    public static final int PT_PHDR = 6;
    public static final int PT_TLS = 7;
    public static final int PT_LOOS = 0x60000000;
    public static final int PT_HIOS = 0x6fffffff;
    public static final int PT_LOPROC = 0x70000000;
    public static final int PT_HIPROC = 0x7fffffff;
    public final int type;
    public final long offset;
    public final long virtual_address;
    public final long physical_address;
    public final long file_size;
    public final long mem_size;
    public final int flags;
    public final long alignment;

    private MemoizedObject<String> ptInterpreter;

    Segment(final Parser parser, long offset) {
        parser.seek(offset);
        if (parser.elfFile.objectSize == ELF.CLASS_32) {
            type = parser.readInt();
            this.offset = parser.readInt();
            virtual_address = parser.readInt();
            physical_address = parser.readInt();
            file_size = parser.readInt();
            mem_size = parser.readInt();
            flags = parser.readInt();
            alignment = parser.readInt();
        } else {
            type = parser.readInt();
            flags = parser.readInt();
            this.offset = parser.readLong();
            virtual_address = parser.readLong();
            physical_address = parser.readLong();
            file_size = parser.readLong();
            mem_size = parser.readLong();
            alignment = parser.readLong();
        }

        switch (type) {
            case PT_INTERP:
                ptInterpreter = new MemoizedObject<String>() {
                    @Override
                    protected String computeValue() throws Exception {
                        parser.seek(Segment.this.offset);
                        StringBuilder buffer = new StringBuilder();
                        int b;
                        while ((b = parser.readUnsignedByte()) != 0)
                            buffer.append((char) b);
                        return buffer.toString();
                    }
                };
                break;
        }
    }

    @Override
    public String toString() {
        String typeString;
        switch (type) {
            case PT_NULL:
                typeString = "PT_NULL";
                break;
            case PT_LOAD:
                typeString = "PT_LOAD";
                break;
            case PT_DYNAMIC:
                typeString = "PT_DYNAMIC";
                break;
            case PT_INTERP:
                typeString = "PT_INTERP";
                break;
            case PT_NOTE:
                typeString = "PT_NOTE";
                break;
            case PT_SHLIB:
                typeString = "PT_SHLIB";
                break;
            case PT_PHDR:
                typeString = "PT_PHDR";
                break;
            default:
                typeString = "0x" + Long.toHexString(type);
                break;
        }

        String pFlagsString = "";
        if (isReadable()) pFlagsString += (pFlagsString.isEmpty() ? "" : "|") + "read";
        if (isWriteable()) pFlagsString += (pFlagsString.isEmpty() ? "" : "|") + "write";
        if (isExecutable()) pFlagsString += (pFlagsString.isEmpty() ? "" : "|") + "execute";

        if (pFlagsString.isEmpty()) pFlagsString = "0x" + Long.toHexString(flags);

        return "ElfProgramHeader[p_type=" + typeString + ", p_filesz=" + file_size + ", p_memsz=" + mem_size + ", p_flags=" + pFlagsString + ", p_align="
                + alignment + ", range=[0x" + Long.toHexString(virtual_address) + "-0x" + Long.toHexString(virtual_address + mem_size) + "]]";
    }

    public String getIntepreter() throws IOException {
        return (ptInterpreter == null) ? null : ptInterpreter.getValue();
    }

    public boolean isReadable() {
        return (flags & /* PF_R= */4) != 0;
    }

    public boolean isWriteable() {
        return (flags & /* PF_W= */2) != 0;
    }

    public boolean isExecutable() {
        return (flags & /* PF_X= */1) != 0;
    }
}
