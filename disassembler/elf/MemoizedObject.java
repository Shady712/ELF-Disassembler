package disassembler.elf;

import java.util.*;
import java.io.*;
import java.nio.*;

public abstract class MemoizedObject<T> {
    private boolean computed;
    private T value;

    protected abstract T computeValue() throws Exception;

    public final T getValue() throws Exception {
        if (!computed) {
            value = computeValue();
            computed = true;
        }
        return value;
    }

    public static <T> MemoizedObject<T>[] uncheckedArray(int size) {
        return new MemoizedObject[size];
    }
}
