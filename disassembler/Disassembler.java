package disassembler;

import disassembler.elf.*;

import java.util.*;
import java.io.*;
import java.nio.*;

public class Disassembler {
    private final ELF file;

    public Disassembler(ELF file) {
        if (file.objectSize != ELF.CLASS_32) {
            throw new InputMismatchException("The file is not a 32 bit elf.");
        }
        if (file.arch != 0xF3) {
            throw new InputMismatchException("The provided elf is not RISC-V architecture.");
        }
        this.file = file;
    }

    public void createOutput(OutputStreamWriter output) {
        PrintWriter writer = new PrintWriter(output);
        Section text = file.firstSectionByName(".text");
        if (text == null) {
            throw new AssertionError("There is no .text part in the provided elf file");
        }
        file.getSymbolTableSection();
        file.getDynamicSymbolTableSection();
        file.parser.seek(text.header.section_offset);
        for (long pos = 0; pos < text.header.size; pos += 4) {
            long address = pos + text.header.address;
            writer.print(String.format("%08X:", address));
            Symbol symbol = file.getELFSymbol(address);
            if (symbol != null && symbol.st_value == address && symbol.section_type == Symbol.STT_FUNC) {
                writer.print('<');
                for (int i = 0; i < 7; i++) {
                    writer.print(' ');
                }
                writer.print(symbol.getName());
                writer.print('>');
            } else {
                for (int i = 0; i < 13; i++) {
                    writer.print(' ');
                }
            }
            int command = file.parser.readInt();
            int rs1 = command >> 15 & ((1 << 5) - 1);
            int rs2 = command >> 20 & ((1 << 5) - 1);
            int imm110 = command >> 20 & ((1 << 12) - 1);
            int funct3 = command >> 12 & ((1 << 3) - 1);
            int funct7 = command >> 25;
            int rd = command >> 7 & ((1 << 5) - 1);
            int operationCode = command & ((1 << 7) - 1);
            if (command == 0b1110011) {
                writer.printf("%8s", "ecall");
                writer.println();
            } else if (operationCode == 0b0010111) {
                writer.printf("%8s", "auipc");
                writer.print("\t" + registerToString(rd) + ", ");
                String tmp = Integer.toUnsignedString((command >>> 12) << 12);
                writer.print(tmp);
                writer.println();
            } else if (operationCode == 0b0110111) {
                writer.printf("%8s", "lui");
                writer.print("\t" + registerToString(rd) + ", ");
                String tmp = Integer.toUnsignedString((command >>> 12) << 12);
                writer.print(tmp);
                writer.println();
            } else if (operationCode == 0b1101111) {
                int tmp = bitMagic1(command >> 12);
                if ((tmp & (1 << 20)) != 0) {
                    tmp = -tmp & ((1 << 20) - 1);
                }
                writer.printf("%8s", "jal");
                writer.print("\t" + registerToString(rd) + ", ");
                writer.print(tmp + "\t");
                writer.print('#');
                writer.print(symbolToString(address + tmp));
                writer.println();
            } else if (operationCode == 0b0000011) {
                String out = getInstruction(operationCode, funct3, false);
                writer.printf("%8s", out);
                writer.print("\t" + registerToString(rd) + ", ");
                writer.print(imm110 + "(");
                writer.print(registerToString(rs1) + ")");
                writer.println();
            } else if (operationCode == 0b0100011) {
                String out = getInstruction(operationCode, funct3, false);
                writer.printf("%8s", out);
                writer.print("\t" + registerToString(rs2) + ", ");
                writer.print((rd | ((imm110 >>> 5) << 5)) + "(");
                writer.print(registerToString(rs1));
                writer.println(")");
            } else if (operationCode == 0b1100011) {
                int tmp = bitMagic2(command);
                if ((tmp & (1 << 12)) != 0) {
                    tmp = -tmp & ((1 << 12) - 1);
                }
                String out = getInstruction(operationCode, funct3, false);
                writer.printf("%8s", out);
                writer.print("\t" + registerToString(rs1) + ", ");
                writer.print(registerToString(rs2) + ", ");
                writer.print(tmp + "\t#");
                writer.print(symbolToString(address + tmp));
                writer.println();
            } else if (operationCode == 0b1100111 && funct3 == 0b000) {
                if ((imm110 & (1 << 11)) != 0) {
                    imm110 = -imm110 & ((1 << 11) - 1);
                }
                writer.printf("%8s", "jalr");
                writer.print("\t" + registerToString(rd) + ", ");
                writer.print(registerToString(rs1) + ", ");
                writer.print(imm110);
                writer.println();
            } else if (operationCode == 0b0010011) {
                if (funct3 == 0b001) {
                    writer.printf("%8s", "slli");
                    writer.print("\t" + registerToString(rd) + ", ");
                    writer.print(registerToString(rs1) + ", ");
                    writer.print(imm110);
                    writer.println();
                } else if (funct3 == 0b101) {
                    if (funct7 == 0b0100000) {
                        writer.printf("%8s", "srai");
                        writer.print("\t" + registerToString(rd) + ", ");
                        writer.print(registerToString(rs1) + ", ");
                        writer.print((imm110 & ((1 << 5) - 1)));
                        writer.println();
                    } else {
                        writer.printf("%8s", "srli");
                        writer.print("\t" + registerToString(rd) + ", ");
                        writer.print(registerToString(rs1) + ", ");
                        writer.print(imm110);
                        writer.println();
                    }
                } else {
                    String out = getInstruction(operationCode, funct3, false);
                    writer.printf("%8s", out);
                    writer.print("\t" + registerToString(rd) + ", ");
                    writer.print(registerToString(rs1) + ", ");
                    writer.print(imm110);
                    writer.println();
                }
            } else if (operationCode == 0b110011) {
                if (funct7 == 0b0100000) {
                    String out = null;
                    if (funct3 == 0) {
                        out = new String("sub");
                    }
                    if (funct3 == 5) {
                        out = new String("sra");
                    }
                    if (out == null) {
                        throw new InputMismatchException("Unknown command");
                    }
                    writer.printf("%8s", out);
                    writer.print("\t" + registerToString(rd) + ", ");
                    writer.print(registerToString(rs2) + ", ");
                    writer.print(registerToString(rs1));
                    writer.println();
                } else if (funct7 == 0) {
                    String out = getInstruction(operationCode, funct3, false);
                    writer.printf("%8s", out);
                    writer.print("\t" + registerToString(rd) + ", ");
                    writer.print(registerToString(rs1) + ", ");
                    writer.print(registerToString(rs2));
                    writer.println();
                } else if (funct7 == 1) {
                    String out = getInstruction(operationCode, funct3, true);
                    writer.printf("%8s", out);
                    writer.print("\t" + registerToString(rd) + ", ");
                    writer.print(registerToString(rs1) + ", ");
                    writer.print(registerToString(rs2));
                    writer.println();
                }
            } else {
                throw new InputMismatchException("Unknown command");
            }
        }
        writer.flush();
    }

    private String symbolToString(long code) {
        Symbol symbol = file.getELFSymbol(code);
        String codeToString = String.format("0x%08X", code);
        if (symbol != null &&
                symbol.section_type == Symbol.STT_FUNC &&
                symbol.st_value == code) {
            codeToString += " <" + symbol.getName() + ">";
        }
        return codeToString;
    }

    private String getInstruction(int operationCode, int index, boolean funct7) {
        if (index > 7) {
            throw new InputMismatchException("Unknown command");
        }
        String[] instructions = new String[8];
        if (operationCode == 0b1100011) {
            instructions = new String[]{"beq", "bne", "????", "????", "blt", "bge", "bltu", "bgeu"};
        }
        if (operationCode == 0b0000011) {
            instructions = new String[]{"lb", "lh", "lw", "???", "lbu", "lhu", "???", "???"};
        }
        if (operationCode == 0b0100011) {
            instructions = new String[]{"sb", "sh", "sw", "??", "??", "??", "??", "??"};
        }
        if (operationCode == 0b0010011) {
            instructions = new String[]{"addi", "????", "slti", "sltiu", "xori", "????", "ori", "andi"};
        }
        if (operationCode == 0b110011) {
            if (funct7) {
                instructions = new String[]{"mul", "mulh", "mulhsu", "mulhu", "div", "divu", "rem", "remu"};
            } else {
                instructions = new String[]{"add", "sll", "slt", "sltu", "xor", "srl", "or", "and"};
            }
        }
        String ans = instructions[index];
        if (ans == null || ans.charAt(0) == '?') {
            throw new InputMismatchException("Unknown command");
        }
        return ans;
    }

    private int bitMagic1(int x) {
        return (((x >>> 9) & ((1 << 10) - 1)) << 1) | (((x >>> 8) & 1) << 11) |
                ((x & ((1 << 8) - 1)) << 12) | (((x >>> 19) & 1) << 20);
    }

    private int bitMagic2(int x) {
        return (((x >>> 8) & ((1 << 4) - 1)) << 1) | (((x >>> 25) & ((1 << 6) - 1)) << 5) |
                (((x >>> 7) & 1) << 11) | (((x >>> 31) & 1) << 12);
    }

    private String registerToString(int reg) {
        if (reg == 0)
            return "zero";
        if (reg == 1)
            return "ra";
        if (reg == 2)
            return "sp";
        if (reg == 3)
            return "gp";
        if (reg == 4)
            return "tp";
        if (5 <= reg && reg <= 7)
            return "t" + (reg - 5);
        if (reg == 8)
            return "s0";
        if (reg == 9)
            return "s1";
        if (10 <= reg && reg <= 17)
            return "a" + (reg - 10);
        if (18 <= reg && reg <= 27)
            return "s" + (reg - 16);
        if (28 <= reg && reg <= 31)
            return "t" + (reg - 25);
        throw new InputMismatchException("Register" + reg + "is not supported by RISC-V");
    }
}
