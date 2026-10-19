package dev.aikido;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Timeout(10)
public class HelpersTest {
    @TempDir
    Path directory;

    @Test
    public void testOkHttpRequestsReuseConnection() throws Exception {
        Set<InetSocketAddress> clients = ConcurrentHashMap.newKeySet();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            clients.add(exchange.getRemoteAddress());
            byte[] body = "ok".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();
        try {
            for (int i = 0; i < 30; i++) {
                var response = Helpers.makeHttpRequestWithOkHttp("http://127.0.0.1:" + server.getAddress().getPort() + "/");
                assertEquals(200, response.getStatusCode());
                assertEquals("ok", response.getMessage());
            }
            assertEquals(1, clients.size());
        } finally {
            server.stop(0);
        }
    }

    @Test
    public void testHttpErrorClosesUnfinishedResponse() throws Exception {
        try (ServerSocket server = new ServerSocket(0, 1, java.net.InetAddress.getLoopbackAddress())) {
            var executor = Executors.newSingleThreadExecutor();
            try {
                var disconnected = executor.submit(() -> {
                    try (var socket = server.accept()) {
                        socket.setSoTimeout(3000);
                        var reader = new java.io.BufferedReader(new java.io.InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
                        while (!reader.readLine().isEmpty()) {}
                        socket.getOutputStream().write("HTTP/1.1 503 Service Unavailable\r\nContent-Length: 1048576\r\n\r\nno"
                                .getBytes(StandardCharsets.US_ASCII));
                        socket.getOutputStream().flush();
                        return socket.getInputStream().read();
                    }
                });
                var response = Helpers.makeHttpRequest("http://localhost:" + server.getLocalPort() + "/");
                assertEquals(400, response.getStatusCode());
                assertTrue(response.getMessage().contains("503"));
                assertEquals(-1, disconnected.get(4, TimeUnit.SECONDS));
            } finally {
                executor.shutdownNow();
            }
        }
    }

    @Test
    public void testCommandPreservesStdout() throws Exception {
        assumeTrue(Files.isExecutable(Path.of("/bin/sh")));
        Path script = directory.resolve("output.sh");
        Files.writeString(script, "printf 'hello\\n'\necho error-message >&2\nprintf 'done\\n'\n");
        var response = Helpers.executeShellCommand("/bin/sh " + script);
        assertEquals(200, response.getStatusCode());
        assertEquals("hello\ndone\n", response.getMessage());
    }

    @Test
    public void testMissingCommandReturnsError() {
        assumeTrue(Files.isExecutable(Path.of("/bin/sh")));
        var response = Helpers.executeShellCommand("/missing/aikido-command");
        assertEquals(400, response.getStatusCode());
    }
}
