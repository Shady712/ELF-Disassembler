package disassembler.elf;

import java.util.*;
import java.io.*;
import java.nio.*;

public class SectionHeader {
    public static final int SHT_NULL = 0;
    public static final int SHT_PROGBITS = 1;
    public static final int SHT_SYMTAB = 2;
    public static final int SHT_STRTAB = 3;
    public static final int SHT_RELA = 4;
    public static final int SHT_HASH = 5;
    public static final int SHT_DYNAMIC = 6;
    public static final int SHT_NOTE = 7;
    public static final int SHT_NOBITS = 8;
    public static final int SHT_REL = 9;
    public static final int SHT_SHLIB = 10;
    public static final int SHT_DYNSYM = 11;
    public static final int SHT_INIT_ARRAY = 14;
    public static final int SHT_FINI_ARRAY = 15;
    public static final int SHT_PREINIT_ARRAY = 16;
    public static final int SHT_GROUP = 17;
    public static final int SHT_SYMTAB_SHNDX = 18;
    public static final int SHT_GNU_HASH = 0x6ffffff6;
    public static final int SHT_GNU_verdef = 0x6ffffffd;
    public static final int SHT_GNU_verneed = 0x6ffffffe;
    public static final int SHT_GNU_versym = 0x6fffffff;
    public static final int SHT_LOOS = 0x60000000;
    public static final int SHT_HIOS = 0x6fffffff;
    public static final int SHT_LOPROC = 0x70000000;
    public static final int SHT_HIPROC = 0x7fffffff;
    public static final int SHT_LOUSER = 0x80000000;
    public static final int SHT_HIUSER = 0xffffffff;
    public static final short SHN_UNDEF = 0;
    public static final short SHN_LORESERVE = (short) 0xff00;
    public static final short SHN_LOPROC = (short) 0xff00;
    public static final short SHN_HIPROC = (short) 0xff1f;
    public static final short SHN_LOOS = (short) 0xff20;
    public static final short SHN_HIOS = (short) 0xff3f;
    public static final short SHN_ABS = (short) 0xfff1;
    public static final short SHN_COMMON = (short) 0xfff2;
    public static final short SHN_XINDEX = (short) 0xffff;
    public static final short SHN_HIRESERVE = (short) 0xffff;
    public static final int FLAG_WRITE = 0x1;
    public static final int FLAG_ALLOC = 0x2;
    public static final int FLAG_EXEC_INSTR = 0x4;
    public static final int FLAG_MASK = 0xf0000000;
    public static final String NAME_STRTAB = ".strtab";
    public static final String NAME_DYNSTR = ".dynstr";
    public static final String NAME_RODATA = ".rodata";
    public final int name_ndx;
    public final int type;
    public final long flags;
    public final long address;
    public final long section_offset;
    public final long size;
    public final int link;
    public final int info;
    public final long address_alignment;
    public final long entry_size;

    private final ELF elfHeader;

    SectionHeader(final Parser parser, long offset) {
        this.elfHeader = parser.elfFile;
        parser.seek(offset);
        name_ndx = parser.readInt();
        type = parser.readInt();
        flags = parser.readIntOrLong();
        address = parser.readIntOrLong();
        section_offset = parser.readIntOrLong();
        size = parser.readIntOrLong();
        link = parser.readInt();
        info = parser.readInt();
        address_alignment = parser.readIntOrLong();
        entry_size = parser.readIntOrLong();
    }

    public String getName() {
        if (name_ndx == 0) return null;
        StringTable tbl = elfHeader.getSectionNameStringTable();
        return tbl.get(name_ndx);
    }

    @Override
    public String toString() {
        return "ElfSectionHeader[name=" + getName() + ", type=0x" + Long.toHexString(type) + "]";
    }
}
