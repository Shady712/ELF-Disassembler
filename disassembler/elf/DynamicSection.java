package disassembler.elf;

import java.util.*;
import java.io.*;
import java.nio.*;

public class DynamicSection extends Section {
    public static final int DT_NULL = 0;
    public static final int DT_NEEDED = 1;
    public static final int DT_PLTRELSZ = 2;
    public static final int DT_PLTGOT = 3;
    public static final int DT_HASH = 4;
    public static final int DT_STRTAB = 5;
    public static final int DT_SYMTAB = 6;
    public static final int DT_RELA = 7;
    public static final int DT_RELASZ = 8;
    public static final int DT_RELAENT = 9;
    public static final int DT_STRSZ = 10;
    public static final int DT_SYMENT = 11;
    public static final int DT_INIT = 12;
    public static final int DT_FINI = 13;
    public static final int DT_SONAME = 14;
    public static final int DT_RPATH = 15;
    public static final int DT_SYMBOLIC = 16;
    public static final int DT_REL = 17;
    public static final int DT_RELSZ = 18;
    public static final int DT_RELENT = 19;
    public static final int DT_PLTREL = 20;
    public static final int DT_DEBUG = 21;
    public static final int DT_TEXTREL = 22;
    public static final int DT_JMPREL = 23;
    public static final int DT_BIND_NOW = 24;
    public static final int DT_INIT_ARRAY = 25;
    public static final int DT_FINI_ARRAY = 26;
    public static final int DT_INIT_ARRAYSZ = 27;
    public static final int DT_FINI_ARRAYSZ = 28;
    public static final int DT_RUNPATH = 29;
    public static final int DT_FLAGS = 30;
    public static final int DT_PREINIT_ARRAY = 32;
    public static final int DT_GNU_HASH = 0x6ffffef5;
    public static final int DT_FLAGS_1 = 0x6ffffffb;
    public static final int DT_VERDEF = 0x6ffffffc;
    public static final int DT_VERDEFNUM = 0x6ffffffd;
    public static final int DT_VERNEEDED = 0x6ffffffe;
    public static final int DT_VERNEEDNUM = 0x6fffffff;
    public static final int DF_ORIGIN = 0x1;
    public static final int DF_SYMBOLIC = 0x2;
    public static final int DF_TEXTREL = 0x4;
    public static final int DF_BIND_NOW = 0x8;
    public static final int DF_1_NOW = 0x00000001;
    public static final int DF_1_GLOBAL = 0x00000002;
    public static final int DF_1_GROUP = 0x00000004;
    public static final int DF_1_NODELETE = 0x00000008;
    public static final int DF_1_LOADFLTR = 0x00000010;
    public static final int DF_1_INITFIRST = 0x00000020;
    public static final int DF_1_NOOPEN = 0x00000040;
    public static final int DF_1_ORIGIN = 0x00000080;
    public static final int DF_1_DIRECT = 0x00000100;
    public static final int DF_1_TRANS = 0x00000200;
    public static final int DF_1_INTERPOSE = 0x00000400;
    public static final int DF_1_NODEFLIB = 0x00000800;
    public static final int DF_1_NODUMP = 0x00001000;
    public static final int DF_1_CONFALT = 0x00002000;
    public static final int DF_1_ENDFILTEE = 0x00004000;
    public static final int DF_1_DISPRELDNE = 0x00008000;
    public static final int DF_1_DISPRELPND = 0x00010000;
    public static final int DF_1_NODIRECT = 0x00020000;
    public static final int DF_1_IGNMULDEF = 0x00040000;
    public static final int DF_1_NOKSYMS = 0x00080000;
    public static final int DF_1_NOHDR = 0x00100000;
    public static final int DF_1_EDITED = 0x00200000;
    public static final int DF_1_NORELOC = 0x00400000;
    public static final int DF_1_SYMINTPOSE = 0x00800000;
    public static final int DF_1_GLOBAUDIT = 0x01000000;
    public static final int DF_1_SINGLETON = 0x02000000;
    public static final int DF_1_STUB = 0x04000000;
    public static final int DF_1_PIE = 0x08000000;

    public long dt_strtab_offset;

    public int dt_strtab_size;

    private MemoizedObject<StringTable> dtStringTable;
    public final List<DynamicStructure> entries = new ArrayList<>();

    public static class DynamicStructure {
        public DynamicStructure(long d_tag, long d_val_or_ptr) {
            this.tag = d_tag;
            this.d_val_or_ptr = d_val_or_ptr;
        }
        public final long tag;
        public final long d_val_or_ptr;
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + (int) (tag ^ (tag >>> 32));
            result = prime * result + (int) (d_val_or_ptr ^ (d_val_or_ptr >>> 32));
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            DynamicStructure other = (DynamicStructure) obj;
            if (tag != other.tag) return false;
            return d_val_or_ptr == other.d_val_or_ptr;
        }

        @Override
        public String toString() {
            return "ElfDynamicSectionEntry{tag=" + tag + ", d_val_or_ptr=" + d_val_or_ptr + "}";
        }
    }

    public DynamicSection(final Parser parser, SectionHeader header) {
        super(parser, header);

        parser.seek(header.section_offset);
        int numEntries = (int) (header.size / 8);
        loop:
        for (int i = 0; i < numEntries; i++) {
            long d_tag = parser.readIntOrLong();
            final long d_val_or_ptr = parser.readIntOrLong();
            entries.add(new DynamicStructure(d_tag, d_val_or_ptr));
            switch ((int) d_tag) {
                case DT_NULL:
                    break loop;
                case DT_STRTAB: {
                    dtStringTable = new MemoizedObject<StringTable>() {
                        @Override
                        protected StringTable computeValue() throws Exception {
                            long fileOffsetForStringTable = parser.virtualMemoryAddrToFileOffset(d_val_or_ptr);
                            return new StringTable(parser, fileOffsetForStringTable, dt_strtab_size, null);
                        }
                    };
                    dt_strtab_offset = d_val_or_ptr;
                }
                break;
                case DT_STRSZ:
                    if (d_val_or_ptr > Integer.MAX_VALUE) throw new Exception("Too large DT_STRSZ: " + d_val_or_ptr);
                    dt_strtab_size = (int) d_val_or_ptr;
                    break;
            }
        }

    }

    private DynamicStructure firstEntryWithTag(long desiredTag) {
        for (DynamicStructure entry : this.entries) {
            if (entry.tag == desiredTag) return entry;
        }
        return null;
    }

    public List<String> getNeededLibraries() throws Exception {
        StringTable stringTable = dtStringTable.getValue();
        List<String> result = new ArrayList<>();
        for (DynamicStructure entry : this.entries) {
            if (entry.tag == DT_NEEDED) result.add(stringTable.get((int) entry.d_val_or_ptr));
        }
        return result;
    }

    public String getRunPath() {
        DynamicStructure runPathEntry = firstEntryWithTag(DT_RUNPATH);
        return runPathEntry == null ? null : dtStringTable.getValue().get((int) runPathEntry.d_val_or_ptr);
    }

    public long getFlags() {
        DynamicStructure flagsEntry = firstEntryWithTag(DT_FLAGS);
        return flagsEntry == null ? 0 : flagsEntry.d_val_or_ptr;
    }

    public long getFlags1() {
        DynamicStructure flagsEntry = firstEntryWithTag(DT_FLAGS_1);
        return flagsEntry == null ? 0 : flagsEntry.d_val_or_ptr;
    }

    @Override
    public String toString() {
        return "ElfDynamicStructure{entries=" + this.entries + "}";
    }
}
