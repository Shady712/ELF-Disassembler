package disassembler.elf;

import java.util.*;
import java.io.*;
import java.nio.*;

public class Symbol {
    public enum Visibility {
        STV_DEFAULT, STV_INTERNAL, STV_HIDDEN, STV_PROTECTED
    }
    public static final int BINDING_LOCAL = 0;
    public static final int BINDING_GLOBAL = 1;
    public static final int BINDING_WEAK = 2;
    public static final int BINDING_LOPROC = 13;
    public static final int BINDING_HIPROC = 15;
    public static final byte STT_NOTYPE = 0;
    public static final byte STT_OBJECT = 1;
    public static final byte STT_FUNC = 2;
    public static final byte STT_SECTION = 3;
    public static final byte STT_FILE = 4;
    public static final byte STT_COMMON = 5;
    public static final byte STT_TLS = 6;
    public static final byte STT_LOOS = 10;
    public static final byte STT_HIOS = 12;
    public static final byte STT_LOPROC = 13;
    public static final byte STT_HIPROC = 15;
    public final int st_name;
    public final long st_value;
    public final long st_size;
    public final short st_info;
    public final short st_other;
    public final short st_shndx;
    public final int section_type;
    public final long offset;
    private final ELF elfHeader;

    Symbol(Parser parser, long offset, int section_type) {
        this.elfHeader = parser.elfFile;
        parser.seek(offset);
        this.offset = offset;
        if (parser.elfFile.objectSize == ELF.CLASS_32) {
            st_name = parser.readInt();
            st_value = parser.readInt();
            st_size = parser.readInt();
            st_info = parser.readUnsignedByte();
            st_other = parser.readUnsignedByte();
            st_shndx = parser.readShort();
        } else {
            st_name = parser.readInt();
            st_info = parser.readUnsignedByte();
            st_other = parser.readUnsignedByte();
            st_shndx = parser.readShort();
            st_value = parser.readLong();
            st_size = parser.readLong();
        }
        this.section_type = section_type;
        switch (getType()) {
            case STT_NOTYPE:
                break;
            case STT_OBJECT:
                break;
            case STT_FUNC:
                break;
            case STT_SECTION:
                break;
            case STT_FILE:
                break;
            case STT_LOPROC:
                break;
            case STT_HIPROC:
                break;
            default:
                break;
        }
    }

    public int getBinding() {
        return st_info >> 4;
    }

    public int getType() {
        return st_info & 0x0F;
    }

    public String getName() throws Exception {
        if (st_name == 0) return null;
        String symbol_name = null;
        if (section_type == SectionHeader.SHT_SYMTAB) {
            symbol_name = elfHeader.getStringTable().get(st_name);
        } else if (section_type == SectionHeader.SHT_DYNSYM) {
            symbol_name = elfHeader.getDynamicStringTable().get(st_name);
        }
        return symbol_name;
    }

    public Visibility getVisibility() {
        if (st_other < 0 || st_other > 3) throw new Exception("Unsupported st_other=" + st_other);
        return Visibility.values()[st_other];
    }

    @Override
    public String toString() {
        String typeString;
        int typeInt = getType();
        switch (typeInt) {
            case STT_NOTYPE:
                typeString = "unspecified";
                break;
            case STT_OBJECT:
                typeString = "object";
                break;
            case STT_FUNC:
                typeString = "function";
                break;
            case STT_SECTION:
                typeString = "section";
                break;
            case STT_FILE:
                typeString = "file";
                break;
            case STT_LOPROC:
                typeString = "loproc";
                break;
            case STT_HIPROC:
                typeString = "hiproc";
                break;
            default:
                typeString = Integer.toString(typeInt);
                break;
        }
        return "ElfSymbol[name=" + getName() + ", type=" + typeString + ", size=" + st_size + "]";
    }
}
