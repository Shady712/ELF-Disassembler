package disassembler.elf;

import java.util.*;
import java.io.*;
import java.nio.*;

public final class ELF {
    public static final int ET_REL = 1;
    public static final int ET_EXEC = 2;
    public static final int ET_DYN = 3;
    public static final int ET_CORE = 4;
    public static final byte CLASS_32 = 1;
    public static final byte CLASS_64 = 2;
    public static final byte DATA_LSB = 1;
    public static final byte DATA_MSB = 2;
    public static final int ARCH_NONE = 0;
    public static final int ARCH_ATT = 1;
    public static final int ARCH_SPARC = 2;
    public static final int ARCH_i386 = 3;
    public static final int ARCH_68k = 4;
    public static final int ARCH_88k = 5;
    public static final int ARCH_i860 = 7;
    public static final int ARCH_MIPS = 8;
    public static final int ARCH_ARM = 0x28;
    public static final int ARCH_X86_64 = 0x3E;
    public static final int ARCH_AARCH64 = 0xB7;
    public final short e_type;
    public final byte objectSize;
    public final byte encoding;
    public final byte elfVersion;
    public final byte abi;
    public final byte abiVersion;
    public final short arch;
    public final int version;
    public final long entry_point;
    public final long ph_offset;
    public final long sh_offset;
    public final int flags;
    public final short eh_size;
    public final short ph_entry_size;
    public final short num_ph;
    public final short sh_entry_size;
    public final short num_sh;
    private short sh_string_ndx;
    private MemoizedObject<Section>[] sections;
    private MemoizedObject<Segment>[] programHeaders;
    private SymbolTableSection symbolTableSection;
    private SymbolTableSection dynamicSymbolTableSection;
    private DynamicSection dynamicSection;
    public Section getSection(int index) throws Exception {
        return sections[index].getValue();
    }

    public List<Section> sectionsOfType(int sectionType) throws Exception {
        if (num_sh < 2) return Collections.emptyList();
        List<Section> result = new ArrayList<>();
        for (int i = 1; i < num_sh; i++) {
            Section section = getSection(i);
            if (section.header.type == sectionType) {
                result.add(section);
            }
        }
        return result;
    }

    public StringTable getSectionNameStringTable() throws Exception {
        return (StringTable) getSection(sh_string_ndx);
    }

    public StringTable getStringTable() throws Exception {
        return findStringTableWithName(SectionHeader.NAME_STRTAB);
    }

    public StringTable getDynamicStringTable() throws Exception {
        return findStringTableWithName(SectionHeader.NAME_DYNSTR);
    }

    private StringTable findStringTableWithName(String tableName) throws Exception {
        return (StringTable) firstSectionByName(tableName);
    }

    public SymbolTableSection getSymbolTableSection() throws Exception {
        return (symbolTableSection != null) ? symbolTableSection : (symbolTableSection = (SymbolTableSection) firstSectionByType(SectionHeader.SHT_SYMTAB));
    }

    public SymbolTableSection getDynamicSymbolTableSection() throws Exception {
        return (dynamicSymbolTableSection != null) ? dynamicSymbolTableSection : (dynamicSymbolTableSection = (SymbolTableSection) firstSectionByType(SectionHeader.SHT_DYNSYM));
    }

    public DynamicSection getDynamicSection() {
        return (dynamicSection != null) ? dynamicSection : (dynamicSection = (DynamicSection) firstSectionByType(SectionHeader.SHT_DYNAMIC));
    }

    public Section firstSectionByType(int type) throws Exception {
        for (int i = 1; i < num_sh; i++) {
            Section sh = getSection(i);
            if (sh.header.type == type) return sh;
        }
        return null;
    }

    public <T extends Section> T firstSectionByType(Class<T> type) throws Exception {
        for (int i = 1; i < num_sh; i++) {
            Section sh = getSection(i);
            if (type.isInstance(sh)) return (T) sh;
        }
        return null;
    }

    public Section firstSectionByName(String sectionName) throws Exception {
        for (int i = 1; i < num_sh; i++) {
            Section sh = getSection(i);
            if (sectionName.equals(sh.header.getName())) return sh;
        }
        return null;
    }

    public Symbol getELFSymbol(String symbolName) throws Exception, IOException {
        if (symbolName == null) return null;

        SymbolTableSection sh = getDynamicSymbolTableSection();
        if (sh != null) {
            int numSymbols = sh.symbols.length;
            for (int i = 0; i < Math.ceil(numSymbols / 2); i++) {
                Symbol symbol = sh.symbols[i];
                if (symbolName.equals(symbol.getName())) {
                    return symbol;
                } else if (symbolName.equals((symbol = sh.symbols[numSymbols - 1 - i]).getName())) {
                    return symbol;
                }
            }
        }

        sh = getSymbolTableSection();
        if (sh != null) {
            int numSymbols = sh.symbols.length;
            for (int i = 0; i < Math.ceil(numSymbols / 2); i++) {
                Symbol symbol = sh.symbols[i];
                if (symbolName.equals(symbol.getName())) {
                    return symbol;
                } else if (symbolName.equals((symbol = sh.symbols[numSymbols - 1 - i]).getName())) {
                    return symbol;
                }
            }
        }
        return null;
    }

    public Symbol getELFSymbol(long address) throws Exception {
        Symbol symbol;
        long value;

        SymbolTableSection sh = getDynamicSymbolTableSection();
        if (sh != null) {
            int numSymbols = sh.symbols.length;
            for (int i = 0; i < numSymbols; i++) {
                symbol = sh.symbols[i];
                value = symbol.st_value;
                if (address >= value && address < value + symbol.st_size) return symbol;
            }
        }

        sh = getSymbolTableSection();
        if (sh != null) {
            int numSymbols = sh.symbols.length;
            for (int i = 0; i < numSymbols; i++) {
                symbol = sh.symbols[i];
                value = symbol.st_value;
                if (address >= value && address < value + symbol.st_size) return symbol;
            }
        }
        return null;
    }

    public Segment getProgramHeader(int index) {
        return programHeaders[index].getValue();
    }

    public static ELF from(InputStream in) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int totalRead = 0;
        byte[] buffer = new byte[8096];
        boolean firstRead = true;
        while (true) {
            int readNow = in.read(buffer, totalRead, buffer.length - totalRead);
            if (readNow == -1) {
                return from(baos.toByteArray());
            } else {
                if (firstRead) {
                    if (readNow < 4) {
                        throw new Exception("Bad first read");
                    } else {
                        if (!(0x7f == buffer[0] && 'E' == buffer[1] && 'L' == buffer[2] && 'F' == buffer[3]))
                            throw new Exception("Bad magic number for file");
                    }
                    firstRead = false;
                }
                baos.write(buffer, 0, readNow);
            }
        }
    }

    public static ELF from(File file) throws Exception, IOException {
        byte[] buffer = new byte[(int) file.length()];
        try (FileInputStream in = new FileInputStream(file)) {
            int totalRead = 0;
            while (totalRead < buffer.length) {
                int readNow = in.read(buffer, totalRead, buffer.length - totalRead);
                if (readNow == -1) {
                    throw new Exception("Premature end of file");
                } else {
                    totalRead += readNow;
                }
            }
        }
        return from(buffer);
    }

    public static ELF from(byte[] buffer) throws Exception, IOException {
        return new ELF(new BackingFile(new ByteArrayInputStream(buffer)));
    }

    public static ELF from(MappedByteBuffer mappedByteBuffer) throws Exception, IOException {
        return new ELF(new BackingFile(mappedByteBuffer));
    }

    public final Parser parser;

    private ELF(BackingFile backingFile) throws Exception, IOException {
        parser = new Parser(this, backingFile);

        byte[] ident = new byte[16];
        int bytesRead = parser.read(ident);
        if (bytesRead != ident.length)
            throw new Exception("Error reading elf header (read " + bytesRead + "bytes - expected to read " + ident.length + "bytes)");

        if (!(0x7f == ident[0] && 'E' == ident[1] && 'L' == ident[2] && 'F' == ident[3]))
            throw new Exception("Bad magic number for file");

        objectSize = ident[4];
        if (!(objectSize == CLASS_32 || objectSize == CLASS_64))
            throw new Exception("Invalid object size class: " + objectSize);
        encoding = ident[5];
        if (!(encoding == DATA_LSB || encoding == DATA_MSB)) throw new Exception("Invalid encoding: " + encoding);
        elfVersion = ident[6];
        if (elfVersion != 1) throw new Exception("Invalid elf version: " + elfVersion);
        abi = ident[7];
        abiVersion = ident[8];

        e_type = parser.readShort();
        arch = parser.readShort();
        version = parser.readInt();
        entry_point = parser.readIntOrLong();
        ph_offset = parser.readIntOrLong();
        sh_offset = parser.readIntOrLong();
        flags = parser.readInt();
        eh_size = parser.readShort();
        ph_entry_size = parser.readShort();
        num_ph = parser.readShort();
        sh_entry_size = parser.readShort();
        num_sh = parser.readShort();
        if (num_sh == 0) {
            throw new Exception("e_shnum is SHN_UNDEF(0), which is not supported yet"
                    + " (the actual number of section header table entries is contained in the sh_size field of the section header at index 0)");
        }
        sh_string_ndx = parser.readShort();
        if (sh_string_ndx == /* SHN_XINDEX= */0xffff) {
            throw new Exception("e_shstrndx is SHN_XINDEX(0xffff), which is not supported yet"
                    + " (the actual index of the section name string table section is contained in the sh_link field of the section header at index 0)");
        }

        sections = MemoizedObject.uncheckedArray(num_sh);
        for (int i = 0; i < num_sh; i++) {
            final long sectionHeaderOffset = sh_offset + (i * sh_entry_size);
            sections[i] = new MemoizedObject<>() {
                @Override
                public Section computeValue() throws Exception {
                    SectionHeader elfSectionHeader = new SectionHeader(parser, sectionHeaderOffset);
                    switch (elfSectionHeader.type) {
                        case SectionHeader.SHT_DYNAMIC:
                            return new DynamicSection(parser, elfSectionHeader);
                        case SectionHeader.SHT_SYMTAB:
                        case SectionHeader.SHT_DYNSYM:
                            return new SymbolTableSection(parser, elfSectionHeader);
                        case SectionHeader.SHT_STRTAB:
                            return new StringTable(parser, elfSectionHeader.section_offset, (int) elfSectionHeader.size, elfSectionHeader);
                        case SectionHeader.SHT_HASH:
                            return new HashTable(parser, elfSectionHeader);
                        case SectionHeader.SHT_NOTE:
                            return new NoteSection(parser, elfSectionHeader);
                        case SectionHeader.SHT_RELA:
                            return new RelocationSection(parser, elfSectionHeader);
                        case SectionHeader.SHT_GNU_HASH:
                            return new GNUHashTable(parser, elfSectionHeader);
                        default:
                            return new Section(parser, elfSectionHeader);
                    }
                }
            };
        }

        programHeaders = MemoizedObject.uncheckedArray(num_ph);
        for (int i = 0; i < num_ph; i++) {
            final long programHeaderOffset = ph_offset + (i * ph_entry_size);
            programHeaders[i] = new MemoizedObject<Segment>() {
                @Override
                public Segment computeValue() {
                    return new Segment(parser, programHeaderOffset);
                }
            };
        }
    }

    public String getInterpreter() throws IOException {
        for (MemoizedObject<Segment> programHeader : programHeaders) {
            Segment ph = programHeader.getValue();
            if (ph.type == Segment.PT_INTERP) return ph.getIntepreter();
        }
        return null;
    }
}
