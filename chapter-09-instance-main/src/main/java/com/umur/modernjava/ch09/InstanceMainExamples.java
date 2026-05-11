package com.umur.modernjava.ch09;

/**
 * Demonstrates JEP 512 (fourth preview in Java 25): instance main methods.
 *
 * The launcher accepts:
 *   - public static void main(String[] args)   the traditional form
 *   - public static void main()                 static, no args
 *   - void main(String[] args)                  instance, with args
 *   - void main()                               instance, no args
 *
 * This file is the entry point. It runs the instance-main demonstrations,
 * then delegates to TraditionalMain and EchoArgs to show the contrasting forms.
 */
public class InstanceMainExamples {

    void main() { // (1)
        IO.println("=== Chapter 9: Instance Main Methods ===");
        IO.println();

        IO.println("[1] InstanceMainExamples is using an instance main with no args.");
        IO.println("    No 'public', no 'static', no 'String[] args' on the signature.");
        IO.println();

        greet("World");
        greet("modern Java");
        IO.println();

        IO.println("[2] Calling the traditional public static void main(String[] args):");
        TraditionalMain.main(new String[]{"hello", "from", "args"}); // (2)
        IO.println();

        IO.println("[3] Calling an instance main that takes String[] args:");
        new EchoArgs().main(new String[]{"first", "second", "third"}); // (3)
        IO.println();

        IO.println("[4] The implicit-class form (Hello.java) is excluded from the");
        IO.println("    Maven build. Run it with the source launcher:");
        IO.println("        java --enable-preview --source 25 \\");
        IO.println("             src/main/java/com/umur/modernjava/ch09/Hello.java");
    }

    void greet(String name) { // (4)
        IO.println("    Hello, " + name + ".");
    }
}
