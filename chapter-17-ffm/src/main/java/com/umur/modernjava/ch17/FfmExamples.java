package com.umur.modernjava.ch17;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemoryLayout.PathElement;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SequenceLayout;
import java.lang.foreign.StructLayout;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;

/**
 * Chapter 17: Foreign Function and Memory API examples.
 *
 * Five demos:
 *   1. Off-heap allocation with a confined arena.
 *   2. Struct layout accessed via VarHandle.
 *   3. Sequence of structs (array of struct).
 *   4. Calling strlen from libc.
 *   5. Bounds checking on a small segment.
 *
 * Run with:
 *   mvn -q clean compile exec:java
 *
 * Or directly:
 *   java --enable-native-access=ALL-UNNAMED -cp target/classes \
 *        com.umur.modernjava.ch17.FfmExamples
 */
public final class FfmExamples {

    private FfmExamples() {
    }

    public static void main(String[] args) throws Throwable {
        System.out.println("=== Chapter 17: Foreign Function & Memory API ===");
        System.out.println();

        offHeapAllocation();
        System.out.println();

        structLayoutWithVarHandle();
        System.out.println();

        sequenceOfStructs();
        System.out.println();

        callStrlen();
        System.out.println();

        boundsChecking();
        System.out.println();

        System.out.println("=== done ===");
    }

    /**
     * Demo 1: Allocate an off-heap segment of 10 ints inside a confined arena,
     * write a small sequence into it, read it back.
     */
    private static void offHeapAllocation() {
        System.out.println("[demo 1] off-heap allocation with a confined arena");

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment ints = arena.allocate(ValueLayout.JAVA_INT, 10);

            for (int i = 0; i < 10; i++) {
                ints.setAtIndex(ValueLayout.JAVA_INT, i, i * i);
            }

            System.out.print("  values:");
            for (int i = 0; i < 10; i++) {
                System.out.print(" " + ints.getAtIndex(ValueLayout.JAVA_INT, i));
            }
            System.out.println();
            System.out.println("  byte size: " + ints.byteSize());
        }
        // memory released here, deterministically
    }

    /**
     * Demo 2: Define a Point struct (int x, int y), allocate one, populate
     * its fields through derived VarHandles, read them back.
     */
    private static void structLayoutWithVarHandle() {
        System.out.println("[demo 2] struct layout with VarHandle");

        StructLayout pointLayout = MemoryLayout.structLayout(
                ValueLayout.JAVA_INT.withName("x"),
                ValueLayout.JAVA_INT.withName("y")
        );

        VarHandle xHandle = pointLayout.varHandle(PathElement.groupElement("x"));
        VarHandle yHandle = pointLayout.varHandle(PathElement.groupElement("y"));

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment point = arena.allocate(pointLayout);

            xHandle.set(point, 0L, 10);
            yHandle.set(point, 0L, 20);

            int x = (int) xHandle.get(point, 0L);
            int y = (int) yHandle.get(point, 0L);

            System.out.println("  layout byte size: " + pointLayout.byteSize());
            System.out.println("  point: x=" + x + ", y=" + y);
        }
    }

    /**
     * Demo 3: Allocate a contiguous array of 5 Point structs and walk it
     * with a sequence-element + group-element access path.
     */
    private static void sequenceOfStructs() {
        System.out.println("[demo 3] sequence of structs");

        StructLayout pointLayout = MemoryLayout.structLayout(
                ValueLayout.JAVA_INT.withName("x"),
                ValueLayout.JAVA_INT.withName("y")
        );
        SequenceLayout points5 = MemoryLayout.sequenceLayout(5, pointLayout);

        VarHandle xHandle = points5.varHandle(
                PathElement.sequenceElement(),
                PathElement.groupElement("x"));
        VarHandle yHandle = points5.varHandle(
                PathElement.sequenceElement(),
                PathElement.groupElement("y"));

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment points = arena.allocate(points5);

            for (long i = 0; i < 5; i++) {
                xHandle.set(points, 0L, i, (int) (i * 10));
                yHandle.set(points, 0L, i, (int) (i * 10 + 1));
            }

            System.out.println("  total bytes: " + points.byteSize());
            for (long i = 0; i < 5; i++) {
                int x = (int) xHandle.get(points, 0L, i);
                int y = (int) yHandle.get(points, 0L, i);
                System.out.println("  points[" + i + "] = {x=" + x + ", y=" + y + "}");
            }
        }
    }

    /**
     * Demo 4: The canonical FFM example. Look up strlen from libc, describe
     * its signature, build a downcall handle, invoke it with a UTF-8 C string.
     *
     * strlen returns size_t (64-bit on every modern target) and takes a
     * const char*.
     */
    private static void callStrlen() throws Throwable {
        System.out.println("[demo 4] calling strlen from libc");

        Linker linker = Linker.nativeLinker();
        SymbolLookup stdlib = linker.defaultLookup();

        MemorySegment strlenSymbol = stdlib.find("strlen")
                .orElseThrow(() -> new IllegalStateException(
                        "strlen not found in default lookup"));

        FunctionDescriptor strlenDescriptor = FunctionDescriptor.of(
                ValueLayout.JAVA_LONG,    // return: size_t
                ValueLayout.ADDRESS       // arg: const char*
        );

        MethodHandle strlen = linker.downcallHandle(strlenSymbol, strlenDescriptor);

        String message = "Hello, FFM!";

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment cString = allocateCString(arena, message);
            long length = (long) strlen.invoke(cString);

            System.out.println("  input:  \"" + message + "\"");
            System.out.println("  Java length:   " + message.length());
            System.out.println("  strlen length: " + length);
        }
    }

    /**
     * Demo 5: Bounds checking. A 4-byte segment will not let you read at
     * offset 100. Unsafe would have read garbage; FFM throws.
     */
    private static void boundsChecking() {
        System.out.println("[demo 5] bounds checking");

        try (Arena arena = Arena.ofConfined()) {
            MemorySegment small = arena.allocate(4);

            small.set(ValueLayout.JAVA_INT, 0, 0xCAFEBABE);
            int valid = small.get(ValueLayout.JAVA_INT, 0);
            System.out.printf("  valid read at offset 0: 0x%08X%n", valid);

            try {
                int outOfBounds = small.get(ValueLayout.JAVA_INT, 100);
                System.out.println("  out-of-bounds read returned: " + outOfBounds);
            } catch (IndexOutOfBoundsException e) {
                System.out.println("  out-of-bounds read threw IndexOutOfBoundsException (good)");
            }
        }
    }

    /**
     * Allocate a null-terminated UTF-8 C string in the given arena.
     *
     * Equivalent to the JDK's allocateUtf8String / allocateFrom helpers,
     * spelled out here so the bytes-level shape is visible.
     */
    private static MemorySegment allocateCString(Arena arena, String value) {
        byte[] utf8 = value.getBytes(StandardCharsets.UTF_8);
        MemorySegment segment = arena.allocate(utf8.length + 1L);
        MemorySegment.copy(utf8, 0, segment, ValueLayout.JAVA_BYTE, 0, utf8.length);
        segment.set(ValueLayout.JAVA_BYTE, utf8.length, (byte) 0);
        return segment;
    }
}
