// This file uses the JEP 512 implicitly declared class form.
// There is no 'package' line, no 'class' declaration, no 'public', no 'static',
// no 'String[] args', and no import for IO. The compiler synthesizes an
// unnamed package-private class around these methods.
//
// Run it directly with the source launcher (NOT via Maven):
//
//   java --enable-preview --source 25 \
//        src/main/java/com/umur/modernjava/ch09/Hello.java
//
// Maven excludes this file from the build because the unnamed-package form
// does not fit the conventional source layout.

void main() {
    greet("World");
    greet("modern Java");
}

void greet(String name) {
    IO.println("Hello, " + name + ".");
}
