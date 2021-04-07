package disassembler.elf;

import java.util.*;
import java.io.*;
import java.nio.*;

public class SymbolTableSection extends Section {
    public final Symbol[] symbols;

    public SymbolTableSection(Parser parser, SectionHeader header) {
        super(parser, header);

        int num_entries = (int) (header.size / header.entry_size);
        symbols = new Symbol[num_entries];
        for (int i = 0; i < num_entries; i++) {
            final long symbolOffset = header.section_offset + (i * header.entry_size);
            symbols[i] = new Symbol(parser, symbolOffset, header.type);
        }
    }
}
