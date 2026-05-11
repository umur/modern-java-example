package com.umur.modernjava.ch18;

import java.util.Random;

import jdk.incubator.vector.IntVector;
import jdk.incubator.vector.LongVector;
import jdk.incubator.vector.VectorOperators;
import jdk.incubator.vector.VectorShape;
import jdk.incubator.vector.VectorSpecies;

/**
 * Vector API examples for Chapter 18.
 *
 * Walks through three demos: the platform's preferred lane shape, sum of squares
 * (scalar vs vector), and dot product (scalar vs vector). Each timed demo runs
 * warmup iterations before the measured pass to give the JIT a fair chance.
 *
 * The code uses {@link jdk.incubator.vector.IntVector#SPECIES_PREFERRED}, so the
 * lane count is whatever the host CPU advertises (4 on Apple Silicon NEON, 8 on
 * AVX2, 16 on AVX-512). Run with --add-modules jdk.incubator.vector.
 */
public final class VectorApiExamples {

    private static final VectorSpecies<Integer> SPECIES = IntVector.SPECIES_PREFERRED;

    private VectorApiExamples() {
        // utility
    }

    public static void main(String[] args) {
        printSpeciesInfo();
        runSumOfSquaresBenchmark();
        runDotProductBenchmark();
    }

    // -------- demo 1: species and lane count --------

    private static void printSpeciesInfo() {
        System.out.println("[demo 1] preferred IntVector species");
        System.out.println("  shape       = " + SPECIES.vectorShape());
        System.out.println("  lane count  = " + SPECIES.length());
        System.out.println("  bit size    = " + SPECIES.vectorBitSize());
        System.out.println("  byte size   = " + SPECIES.vectorByteSize());
        System.out.println();
    }

    // -------- demo 2: sum of squares --------

    private static void runSumOfSquaresBenchmark() {
        System.out.println("[demo 2] sum of squares: scalar vs vector");
        int[] data = randomInts(1_000_000, 12345L, 1_000);

        // warmup so the JIT compiles both methods before we time them
        long warmupScalar = 0;
        long warmupVector = 0;
        for (int i = 0; i < 5; i++) {
            warmupScalar ^= sumOfSquaresScalar(data);
            warmupVector ^= sumOfSquaresVector(data);
        }
        // touch the warmup results so the JIT can't dead-code them away
        if (warmupScalar == Long.MIN_VALUE && warmupVector == Long.MIN_VALUE) {
            System.out.println("warmup XOR collision; not actually expected");
        }

        long scalarStart = System.nanoTime();
        long scalarResult = sumOfSquaresScalar(data);
        long scalarNanos = System.nanoTime() - scalarStart;

        long vectorStart = System.nanoTime();
        long vectorResult = sumOfSquaresVector(data);
        long vectorNanos = System.nanoTime() - vectorStart;

        System.out.println("  scalar result = " + scalarResult);
        System.out.println("  vector result = " + vectorResult);
        System.out.println("  results match = " + (scalarResult == vectorResult));
        System.out.println("  scalar time   = " + formatNanos(scalarNanos));
        System.out.println("  vector time   = " + formatNanos(vectorNanos));
        System.out.printf ("  speedup       = %.2fx%n", (double) scalarNanos / vectorNanos);
        System.out.println();
    }

    static long sumOfSquaresScalar(int[] data) {
        long total = 0L;
        for (int i = 0; i < data.length; i++) {
            total += (long) data[i] * data[i];
        }
        return total;
    }

    static long sumOfSquaresVector(int[] data) {
        int upperBound = SPECIES.loopBound(data.length);
        long total = 0L;

        // accumulate into long lanes so a million int^2 values cannot overflow
        VectorSpecies<Long> longSpecies = LongVector.SPECIES_PREFERRED;
        LongVector accumulator = LongVector.zero(longSpecies);

        int laneStep = SPECIES.length();
        for (int i = 0; i < upperBound; i += laneStep) {
            IntVector v = IntVector.fromArray(SPECIES, data, i);
            IntVector squared = v.mul(v);
            // widen the int lanes to long lanes (two halves on platforms where
            // the int and long species have different lane counts)
            int half = longSpecies.length();
            for (int part = 0; part < laneStep; part += half) {
                LongVector widened = (LongVector) squared
                        .convertShape(VectorOperators.I2L, longSpecies, part / half);
                accumulator = accumulator.add(widened);
            }
        }
        total += accumulator.reduceLanes(VectorOperators.ADD);

        // tail loop for any leftover elements
        for (int i = upperBound; i < data.length; i++) {
            total += (long) data[i] * data[i];
        }
        return total;
    }

    // -------- demo 3: dot product --------

    private static void runDotProductBenchmark() {
        System.out.println("[demo 3] dot product: scalar vs vector");
        int[] xs = randomInts(1_000_000, 11111L, 1_000);
        int[] ys = randomInts(1_000_000, 22222L, 1_000);

        long warmupScalar = 0;
        long warmupVector = 0;
        for (int i = 0; i < 5; i++) {
            warmupScalar ^= dotProductScalar(xs, ys);
            warmupVector ^= dotProductVector(xs, ys);
        }
        if (warmupScalar == Long.MIN_VALUE && warmupVector == Long.MIN_VALUE) {
            System.out.println("warmup XOR collision; not actually expected");
        }

        long scalarStart = System.nanoTime();
        long scalarResult = dotProductScalar(xs, ys);
        long scalarNanos = System.nanoTime() - scalarStart;

        long vectorStart = System.nanoTime();
        long vectorResult = dotProductVector(xs, ys);
        long vectorNanos = System.nanoTime() - vectorStart;

        System.out.println("  scalar result = " + scalarResult);
        System.out.println("  vector result = " + vectorResult);
        System.out.println("  results match = " + (scalarResult == vectorResult));
        System.out.println("  scalar time   = " + formatNanos(scalarNanos));
        System.out.println("  vector time   = " + formatNanos(vectorNanos));
        System.out.printf ("  speedup       = %.2fx%n", (double) scalarNanos / vectorNanos);
        System.out.println();
    }

    static long dotProductScalar(int[] xs, int[] ys) {
        long total = 0L;
        for (int i = 0; i < xs.length; i++) {
            total += (long) xs[i] * ys[i];
        }
        return total;
    }

    static long dotProductVector(int[] xs, int[] ys) {
        int upperBound = SPECIES.loopBound(xs.length);
        long total = 0L;

        VectorSpecies<Long> longSpecies = LongVector.SPECIES_PREFERRED;
        LongVector accumulator = LongVector.zero(longSpecies);

        int laneStep = SPECIES.length();
        for (int i = 0; i < upperBound; i += laneStep) {
            IntVector x = IntVector.fromArray(SPECIES, xs, i);
            IntVector y = IntVector.fromArray(SPECIES, ys, i);
            IntVector product = x.mul(y);
            int half = longSpecies.length();
            for (int part = 0; part < laneStep; part += half) {
                LongVector widened = (LongVector) product
                        .convertShape(VectorOperators.I2L, longSpecies, part / half);
                accumulator = accumulator.add(widened);
            }
        }
        total += accumulator.reduceLanes(VectorOperators.ADD);

        for (int i = upperBound; i < xs.length; i++) {
            total += (long) xs[i] * ys[i];
        }
        return total;
    }

    // -------- helpers --------

    private static int[] randomInts(int length, long seed, int bound) {
        Random rng = new Random(seed);
        int[] data = new int[length];
        for (int i = 0; i < length; i++) {
            data[i] = rng.nextInt(bound);
        }
        return data;
    }

    private static String formatNanos(long nanos) {
        if (nanos < 1_000) {
            return nanos + " ns";
        }
        if (nanos < 1_000_000) {
            return String.format("%.2f us", nanos / 1_000.0);
        }
        if (nanos < 1_000_000_000L) {
            return String.format("%.2f ms", nanos / 1_000_000.0);
        }
        return String.format("%.2f s", nanos / 1_000_000_000.0);
    }
}
