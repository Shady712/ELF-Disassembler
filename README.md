# ELF-Disassembler
ELF file disassembler code written on Java.

# ELF
ELF is the executable file for Linux system (and other UNIX-like operation systems). Disassembler is the program which returns human-readable code using binary code from the input in elf format. Special library (disassembler.elf package) is used for the implementation.

# Disassembler
Disassembler parses .text and .symbolTable sections of the provided elf file. During the parsing process, disassembler prints the corresponding commands, arguments, tags and other parts of the code in the provided output file. The resulting code follows the RISC-V processor architecture.

# Main
The Main class uses program arguments to run the disassembler. The first argument of the command line is the name of elf file, the second one is the name of output file (or output is System.out if no second argument is provided). Of course, all possible exceptions are caught.
