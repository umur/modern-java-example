package com.umur.modernjava.ch03;

import java.util.UUID;

public class TextBlockExamples {

    public static void main(String[] args) {
        System.out.println("=== Modern Java in Practice, Chapter 3: Text Blocks ===");
        System.out.println();

        sqlQuery();
        System.out.println();

        jsonBody();
        System.out.println();

        htmlSnippet();
        System.out.println();

        indentationAlgorithm();
        System.out.println();

        lineContinuationEscape();
        System.out.println();

        trailingSpaceEscape();
    }

    // 3.4: SQL queries.
    static void sqlQuery() {
        String query = """
                SELECT o.id, o.customer_id, o.total, c.email
                FROM orders o
                INNER JOIN customers c ON c.id = o.customer_id
                WHERE o.created_at > ?
                  AND o.status = 'PAID'
                ORDER BY o.created_at DESC
                LIMIT 100
                """;
        System.out.println("--- SQL query ---");
        System.out.print(query);
    }

    // 3.4 + 3.5: JSON parameterized with formatted().
    static void jsonBody() {
        UUID customerId = UUID.fromString("a1f93328-e116-43b3-8000-000000000000");
        String sku = "SKU-204";
        int qty = 2;

        String body = """
                {
                  "customerId": "%s",
                  "items": [
                    { "sku": "%s", "qty": %d }
                  ]
                }
                """.formatted(customerId, sku, qty);

        System.out.println("--- JSON body ---");
        System.out.print(body);
    }

    // 3.4: HTML email snippet.
    static void htmlSnippet() {
        UUID orderId = UUID.fromString("00000000-0000-0000-0000-000000000042");
        String emailHtml = """
                <html>
                  <body>
                    <h1>Thanks for your order</h1>
                    <p>Order ID: <strong>%s</strong></p>
                    <p>We'll email you again when it ships.</p>
                  </body>
                </html>
                """.formatted(orderId);

        System.out.println("--- HTML snippet ---");
        System.out.print(emailHtml);
    }

    // 3.3: indentation algorithm. Same body, three closing-quote positions.
    static void indentationAlgorithm() {
        // Closing """ aligned with content -> indent stripped to zero.
        String alignedClose = """
                hello
                """;

        // Closing """ pulled left by 4 columns -> body keeps 4 leading spaces.
        String shortClose = """
                hello
            """;

        // Closing """ pulled all the way to column 0 -> body keeps all
        // 16 source columns of indent (minimum across non-blank lines is 0).
        String flushClose = """
                hello
""";

        System.out.println("--- Indentation algorithm ---");
        System.out.println("aligned close   -> [" + alignedClose.replace("\n", "\\n") + "]");
        System.out.println("short close     -> [" + shortClose.replace("\n", "\\n") + "]");
        System.out.println("flush close     -> [" + flushClose.replace("\n", "\\n") + "]");
    }

    // 3.3: \ at end of line drops the would-be newline.
    static void lineContinuationEscape() {
        String oneLine = """
                the quick brown fox \
                jumps over the lazy dog
                """;
        System.out.println("--- Line continuation (\\) ---");
        System.out.println("[" + oneLine.replace("\n", "\\n") + "]");
    }

    // 3.3: \s preserves a single trailing space the stripping rule would eat.
    static void trailingSpaceEscape() {
        String withoutEscape = """
                Enter your name:
                """;
        String withEscape = """
                Enter your name:\s
                """;

        System.out.println("--- Trailing space (\\s) ---");
        System.out.println("without \\s -> [" + withoutEscape.replace("\n", "\\n") + "]");
        System.out.println("with \\s    -> [" + withEscape.replace("\n", "\\n") + "]");
    }
}
