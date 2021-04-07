package disassembler.elf;

import java.util.*;
import java.io.*;
import java.nio.*;

public class GNUHashTable extends Section {
    private final Parser parser;
    private final int ELFCLASS_BITS;
    int symbolOffset;
    int bloomShift;
    long[] bloomFilter;
    int[] buckets;
    int[] chain;

    GNUHashTable(Parser parser, SectionHeader header) {
        super(parser, header);
        this.parser = parser;
        ELFCLASS_BITS = parser.elfFile.objectSize == ELF.CLASS_32 ? 32 : 64;
        parser.seek(header.section_offset);
        int numberOfBuckets = parser.readInt();
        symbolOffset = parser.readInt();
        int bloomSize = parser.readInt();
        bloomShift = parser.readInt();
        bloomFilter = new long[bloomSize];
        buckets = new int[numberOfBuckets];
        for (int i = 0; i < bloomSize; i++) {
            bloomFilter[i] = parser.readIntOrLong();
        }
        for (int i = 0; i < numberOfBuckets; i++) {
            buckets[i] = parser.readInt();
        }
    }

    Symbol lookupSymbol(String symbolName, SymbolTableSection symbolTable) {
        if (chain == null) {
            int chainSize = ((SymbolTableSection) parser.elfFile.firstSectionByType(SectionHeader.SHT_DYNSYM)).symbols.length - symbolOffset;
            chain = new int[chainSize];
            parser.seek(header.section_offset + 4*4 + bloomFilter.length*(ELFCLASS_BITS/8) + buckets.length * 4);
            for (int i = 0; i < chainSize; i++) {
                chain[i] = parser.readInt();
            }
        }
        final int nameHash = gnuHash(symbolName);
        long word = bloomFilter[(Integer.remainderUnsigned(Integer.divideUnsigned(nameHash, ELFCLASS_BITS), bloomFilter.length))];
        long mask = 1L << (long) (Integer.remainderUnsigned(nameHash, ELFCLASS_BITS))
                | 1L << (long) (Integer.remainderUnsigned((nameHash >>> bloomShift), ELFCLASS_BITS));
        if ((word & mask) != mask) {
            return null;
        }
        int symix = buckets[Integer.remainderUnsigned(nameHash, buckets.length)];
        if (symix < symbolOffset) {
            return null;
        }
        while (true) {
            int hash = chain[symix - symbolOffset];

            if ((((long) nameHash)|1L) == (((long) hash)|1L)) {
                Symbol symbol = symbolTable.symbols[symix];
                if (symbolName.equals(symbol.getName())) return symbol;
            }
            Symbol symbol = symbolTable.symbols[symix];
            if ((hash & 1) != 0) {
                break;
            }
            symix++;
        }
        return null;
    }

    static int gnuHash(String name) {
        int h = 5381;
        int nameLength = name.length();
        for (int i = 0; i < nameLength; i++) {
            char c = name.charAt(i);
            h = (h << 5) + h + c;
        }
        return h;
    }
}
