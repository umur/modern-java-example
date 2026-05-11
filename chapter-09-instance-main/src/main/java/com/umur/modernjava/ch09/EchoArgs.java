package com.umur.modernjava.ch09;

/**
 * Demonstrates the instance-main-with-args form: void main(String[] args).
 *
 * The launcher will call this with command-line arguments, exactly like the
 * traditional static form. The only difference is the 'static' keyword has
 * been dropped, and the launcher constructs an instance to call this on.
 */
public class EchoArgs {

    void main(String[] args) {
        if (args.length == 0) {
            IO.println("    [EchoArgs] no arguments");
            return;
        }
        IO.println("    [EchoArgs] " + String.join(" ", args));
    }
}
