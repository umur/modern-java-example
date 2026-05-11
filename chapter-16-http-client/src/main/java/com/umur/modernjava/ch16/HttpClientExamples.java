package com.umur.modernjava.ch16;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Demonstrates the JDK HTTP client (JEP 321, final since Java 11).
 *
 * The code below uses a `com.sun.net.httpserver.HttpServer` started in-process
 * on a random localhost port, so the demo runs without any external network
 * access. The same `java.net.http.HttpClient` calls would talk to a real
 * remote service unchanged.
 *
 * No --enable-preview required. Builds and runs on JDK 11 or later. The pom in
 * this project targets release 25 to match the rest of the book.
 */
public final class HttpClientExamples {

    public static void main(String[] args) throws Exception {
        ExecutorService serverExecutor = Executors.newFixedThreadPool(8);
        HttpServer server = startStubServer(serverExecutor);
        int port = server.getAddress().getPort();
        String base = "http://localhost:" + port;
        log("stub server up on " + base);

        // One client, reused across every demo. Java 21+ made HttpClient
        // AutoCloseable; closing it shuts down its internal selector and
        // executor threads so the JVM can exit when main returns.
        try (HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build()) {

            section("1. Sync GET with BodyHandlers.ofString()");
            demoSyncGet(client, base);

            section("2. Async GET returning CompletableFuture");
            demoAsyncGet(client, base);

            section("3. POST with text body, custom headers");
            demoPostWithHeaders(client, base);

            section("4. Streaming response as InputStream (4 KB binary)");
            demoStreamingInputStream(client, base);

            section("5. Parallel fanout on virtual threads (5 concurrent GETs)");
            demoVirtualThreadFanout(client, base);

            log("ALL DONE");
        } finally {
            server.stop(0);
            serverExecutor.shutdown();
            log("stub server stopped");
        }
    }

    /**
     * Demo 1: the smallest useful interaction. Build a request, send it, read
     * the body as a String. Three lines of work, one of them shared with the
     * other demos (the client itself).
     */
    static void demoSyncGet(HttpClient client, String base)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/hello"))
                .header("Accept", "text/plain")
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

        log("request:  GET " + request.uri());

        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

        log("response: " + response.statusCode()
                + " " + response.headers().firstValue("content-type").orElse("<no ct>"));
        log("body:     \"" + response.body() + "\"");
    }

    /**
     * Demo 2: same request, async path. `sendAsync` returns immediately with a
     * future; `.join()` blocks until it completes. In real reactive code you'd
     * compose the future, not join. Here we keep the demo linear.
     */
    static void demoAsyncGet(HttpClient client, String base) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/hello"))
                .GET()
                .build();

        log("request:  GET " + request.uri() + "  (sendAsync)");

        CompletableFuture<HttpResponse<String>> future =
                client.sendAsync(request, BodyHandlers.ofString());

        HttpResponse<String> response = future.join();

        log("response: " + response.statusCode());
        log("body:     \"" + response.body() + "\"");
    }

    /**
     * Demo 3: POST with a text body and a couple of custom headers. The body
     * publisher is BodyPublishers.ofString. Note the explicit Content-Type:
     * the JDK client does not set one for you, and forgetting it is one of
     * the most common bugs with this API.
     */
    static void demoPostWithHeaders(HttpClient client, String base)
            throws IOException, InterruptedException {
        String payload = "{\"id\":42,\"action\":\"reserve\"}";

        HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/echo"))
                .header("Content-Type", "application/json")
                .header("X-Trace-Id", "trace-" + ThreadLocalRandom.current().nextInt(10_000))
                .timeout(Duration.ofSeconds(10))
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        log("request:  POST " + request.uri());
        log("payload:  " + payload);
        log("headers:  Content-Type=" + request.headers().firstValue("content-type").orElse(""));
        log("          X-Trace-Id=" + request.headers().firstValue("x-trace-id").orElse(""));

        HttpResponse<String> response = client.send(request, BodyHandlers.ofString());

        log("response: " + response.statusCode());
        log("body:     " + response.body());
    }

    /**
     * Demo 4: read the response body as an InputStream. The endpoint serves
     * 4 KB of pseudo-binary data. We read it in 512-byte chunks to make the
     * point that the body is a live stream, not a pre-buffered String.
     */
    static void demoStreamingInputStream(HttpClient client, String base)
            throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + "/binary?bytes=4096"))
                .GET()
                .build();

        log("request:  GET " + request.uri());

        HttpResponse<InputStream> response = client.send(request, BodyHandlers.ofInputStream());

        log("response: " + response.statusCode());

        try (InputStream body = response.body()) {
            byte[] buffer = new byte[512];
            int total = 0;
            int chunks = 0;
            int read;
            while ((read = body.read(buffer)) != -1) {
                total += read;
                chunks++;
            }
            log("read:     " + total + " bytes in " + chunks + " chunks of up to 512");
        }
    }

    /**
     * Demo 5: five concurrent sync GETs from inside a virtual-thread executor.
     * Each `client.send` blocks its own virtual thread; the JVM unmounts the
     * virtual thread from its carrier while the I/O is in flight. Five HTTP
     * calls run in parallel; the executor close() blocks until all finish.
     *
     * The stub server sleeps a bit on /slow to make the parallelism visible:
     * five sequential 200 ms sleeps would take ~1000 ms; in parallel they
     * finish in ~200 ms.
     */
    static void demoVirtualThreadFanout(HttpClient client, String base) {
        long start = System.nanoTime();

        try (ExecutorService vt = Executors.newVirtualThreadPerTaskExecutor()) {
            List<CompletableFuture<Integer>> futures = List.of(1, 2, 3, 4, 5).stream()
                    .map(i -> CompletableFuture.supplyAsync(() -> {
                        HttpRequest request = HttpRequest.newBuilder(
                                        URI.create(base + "/slow?id=" + i))
                                .GET()
                                .build();
                        try {
                            HttpResponse<String> response =
                                    client.send(request, BodyHandlers.ofString());
                            log("[task " + i + "] -> " + response.statusCode()
                                    + " body=\"" + response.body() + "\"");
                            return response.statusCode();
                        } catch (IOException | InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }, vt))
                    .toList();

            // Wait for all five to complete.
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
        }

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        log("wall time for 5 parallel /slow calls: " + elapsedMs + " ms"
                + "  (5 sequential ~200 ms calls would be ~1000 ms)");
    }

    // ---------- in-process stub server ----------

    /**
     * Starts a minimal HTTP server on a random localhost port. Provides four
     * endpoints used by the demos:
     *   GET  /hello             -> "Hello from the JDK!" as text/plain
     *   POST /echo              -> echoes the request body and headers as text
     *   GET  /binary?bytes=N    -> N pseudo-random bytes of application/octet-stream
     *   GET  /slow?id=X         -> sleeps 200 ms, then returns "slow X"
     */
    static HttpServer startStubServer(ExecutorService executor) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

        server.createContext("/hello", exchange -> {
            byte[] body = "Hello from the JDK!".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(200, body.length);
            try (var os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        server.createContext("/echo", exchange -> {
            String requestBody;
            try (InputStream is = exchange.getRequestBody()) {
                requestBody = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            }
            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            String traceId = exchange.getRequestHeaders().getFirst("X-Trace-Id");
            String response = "echo: method=" + exchange.getRequestMethod()
                    + " content-type=" + contentType
                    + " trace-id=" + traceId
                    + " body=" + requestBody;
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (var os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });

        server.createContext("/binary", exchange -> {
            int bytes = parseIntParam(exchange, "bytes", 4096);
            byte[] payload = new byte[bytes];
            // deterministic-ish, doesn't really matter; just non-zero
            for (int i = 0; i < bytes; i++) {
                payload[i] = (byte) (i & 0xff);
            }
            exchange.getResponseHeaders().set("Content-Type", "application/octet-stream");
            exchange.sendResponseHeaders(200, payload.length);
            try (var os = exchange.getResponseBody()) {
                os.write(payload);
            }
        });

        server.createContext("/slow", exchange -> {
            String id = parseStringParam(exchange, "id", "?");
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            byte[] body = ("slow " + id).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
            exchange.sendResponseHeaders(200, body.length);
            try (var os = exchange.getResponseBody()) {
                os.write(body);
            }
        });

        // The default executor is a single-threaded one that processes
        // requests serially. Swap in a small thread pool so /slow can serve
        // five concurrent requests in demo 5. The caller owns the executor
        // and shuts it down at the end of main().
        server.setExecutor(executor);
        server.start();
        return server;
    }

    static int parseIntParam(HttpExchange exchange, String name, int defaultValue) {
        String raw = parseStringParam(exchange, name, null);
        if (raw == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    static String parseStringParam(HttpExchange exchange, String name, String defaultValue) {
        String query = exchange.getRequestURI().getQuery();
        if (query == null) {
            return defaultValue;
        }
        for (String pair : query.split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0 && pair.substring(0, eq).equals(name)) {
                return pair.substring(eq + 1);
            }
        }
        return defaultValue;
    }

    // ---------- helpers ----------

    static void log(String msg) {
        System.out.println("[" + Thread.currentThread().getName() + "] " + msg);
    }

    static void section(String title) {
        System.out.println();
        System.out.println("==== " + title + " ====");
    }
}
