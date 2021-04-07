package disassembler.elf;

import java.util.*;
import java.io.*;
import java.nio.*;

public class Section {
    public final SectionHeader header;
    private final Parser parser;

    public Section(Parser parser, SectionHeader header) {
        this.header = header;
        this.parser = parser;
    }

    public byte[] rawSection() {
        parser.seek(header.section_offset);
        byte[] data = new byte[(int) header.size];
        parser.read(data);
        return data;
    }
}
