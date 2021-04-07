package disassembler;

import disassembler.elf.ELF;

import java.util.*;
import java.io.*;
import java.nio.*;

public class Main {
    public static void main(String[] args) {
        if (args.length > 2) {
            throw new RuntimeException("Something strange is going to happen");
        }
        if (args.length < 1) {
            throw new RuntimeException("No input elf file provided");
        }
        if (args.length == 1) {
            try (InputStream in = new BufferedInputStream(new FileInputStream(args[0]))) {
                new Disassembler(ELF.from(in)).createOutput(new OutputStreamWriter(System.out));
            } catch (FileNotFoundException e) {
                System.out.println("File not found");
            } catch (IOException e) {
                System.out.println("Invalid input provided");
            }
        } else {
            try (InputStream in = new BufferedInputStream(new FileInputStream(args[0]));
                 OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(args[1]))) {
                new Disassembler(ELF.from(in)).createOutput(writer);
            } catch (FileNotFoundException e) {
                System.out.println("File not found");
            } catch (IOException e) {
                System.out.println("Invalid input provided");
            }
        }
    }
}
