package com.umur.modernjava.ch09;

/**
 * The 35-year-old entry point form, kept for contrast.
 * Every keyword in the signature is meaningful; none of them help a beginner.
 */
public class TraditionalMain {

    public static void main(String[] args) {
        System.out.println("    [TraditionalMain] public static void main(String[] args)");
        System.out.println("    [TraditionalMain] received " + args.length + " arg(s): "
                + String.join(", ", args));
    }
}
