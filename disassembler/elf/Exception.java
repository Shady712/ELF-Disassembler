package disassembler.elf;

import java.util.*;
import java.io.*;
import java.nio.*;

public class Exception extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public Exception(String message) {
        super(message);
    }

    public Exception(Throwable cause) {
        super(cause);
    }

    public Exception(String message, Throwable cause) {
        super(message, cause);
    }
}
