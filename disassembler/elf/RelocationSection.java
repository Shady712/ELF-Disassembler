package disassembler.elf;

import java.util.*;
import java.io.*;
import java.nio.*;

public class RelocationSection extends Section {
    public RelocationSection(Parser parser, SectionHeader header) {
        super(parser, header);
        int num_entries = (int) (header.size / header.entry_size);
    }
}
