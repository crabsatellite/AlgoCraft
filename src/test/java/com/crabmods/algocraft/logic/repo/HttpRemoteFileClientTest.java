package com.crabmods.algocraft.logic.repo;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.InetAddress;
import java.net.ProxySelector;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpRemoteFileClientTest {
    @Test
    void rejectsOversizedContentLengthBeforeReadingBody() throws Exception {
        try (OneShotHttpServer server = OneShotHttpServer.start(output -> {
            String response = "HTTP/1.1 200 OK\r\n"
                    + "Content-Length: " + (RemoteRepositoryDownloader.MAX_REPOSITORY_FILE_BYTES + 1) + "\r\n"
                    + "\r\n";
            output.write(response.getBytes(StandardCharsets.US_ASCII));
        })) {
            IOException error = assertThrows(IOException.class,
                    () -> new HttpRemoteFileClient().get(server.uri()));

            assertTrue(error.getMessage().contains("exceeds maximum size"), error.getMessage());
        }
    }

    @Test
    void rejectsChunkedBodyThatGrowsPastLimitWithoutContentLength() throws Exception {
        try (OneShotHttpServer server = OneShotHttpServer.start(output -> {
            output.write("""
                    HTTP/1.1 200 OK\r
                    Connection: close\r
                    \r
                    """.getBytes(StandardCharsets.US_ASCII));
            byte[] chunk = new byte[8192];
            long remaining = RemoteRepositoryDownloader.MAX_REPOSITORY_FILE_BYTES + 1;
            while (remaining > 0) {
                int size = (int) Math.min(chunk.length, remaining);
                output.write(chunk, 0, size);
                remaining -= size;
            }
        })) {
            IOException error = assertThrows(IOException.class,
                    () -> new HttpRemoteFileClient().get(server.uri()));

            assertTrue(error.getMessage().contains("exceeds maximum size"), error.getMessage());
        }
    }

    @Test
    void closesNonSuccessResponseBodyBeforeThrowing() {
        CloseTrackingInputStream body = new CloseTrackingInputStream();
        HttpRemoteFileClient client = new HttpRemoteFileClient(new FakeHttpClient(
                new FakeResponse(500, HttpHeaders.of(Map.of(), (name, value) -> true), body)));

        IOException error = assertThrows(IOException.class,
                () -> client.get(URI.create("https://example.test/manifest.json")));

        assertTrue(error.getMessage().contains("HTTP 500"), error.getMessage());
        assertTrue(body.closed, "non-200 responses must close the response body stream");
    }

    @Test
    void closesOversizedDeclaredResponseBodyBeforeThrowing() {
        CloseTrackingInputStream body = new CloseTrackingInputStream();
        HttpRemoteFileClient client = new HttpRemoteFileClient(new FakeHttpClient(
                new FakeResponse(200, HttpHeaders.of(Map.of(
                        "Content-Length", List.of(Long.toString(RemoteRepositoryDownloader.MAX_REPOSITORY_FILE_BYTES + 1))
                ), (name, value) -> true), body)));

        IOException error = assertThrows(IOException.class,
                () -> client.get(URI.create("https://example.test/manifest.json")));

        assertTrue(error.getMessage().contains("exceeds maximum size"), error.getMessage());
        assertTrue(body.closed, "oversized declared responses must close the response body stream");
    }

    private interface ResponseWriter {
        void write(OutputStream output) throws IOException;
    }

    private static final class CloseTrackingInputStream extends InputStream {
        private boolean closed;

        @Override
        public int read() {
            return -1;
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private static final class FakeHttpClient extends HttpClient {
        private final HttpResponse<InputStream> response;

        private FakeHttpClient(HttpResponse<InputStream> response) {
            this.response = response;
        }

        @Override
        public Optional<CookieHandler> cookieHandler() {
            return Optional.empty();
        }

        @Override
        public Optional<Duration> connectTimeout() {
            return Optional.empty();
        }

        @Override
        public Redirect followRedirects() {
            return Redirect.NEVER;
        }

        @Override
        public Optional<ProxySelector> proxy() {
            return Optional.empty();
        }

        @Override
        public SSLContext sslContext() {
            return null;
        }

        @Override
        public SSLParameters sslParameters() {
            return null;
        }

        @Override
        public Optional<Authenticator> authenticator() {
            return Optional.empty();
        }

        @Override
        public Version version() {
            return Version.HTTP_1_1;
        }

        @Override
        public Optional<Executor> executor() {
            return Optional.empty();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) {
            return (HttpResponse<T>) response;
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
                                                                HttpResponse.BodyHandler<T> responseBodyHandler) {
            throw new UnsupportedOperationException("not used");
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
                                                                HttpResponse.BodyHandler<T> responseBodyHandler,
                                                                HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
            throw new UnsupportedOperationException("not used");
        }
    }

    private record FakeResponse(int statusCode, HttpHeaders headers, InputStream body)
            implements HttpResponse<InputStream> {
        @Override
        public HttpRequest request() {
            return null;
        }

        @Override
        public Optional<HttpResponse<InputStream>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return URI.create("https://example.test/manifest.json");
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }
    }

    private static final class OneShotHttpServer implements AutoCloseable {
        private final ServerSocket serverSocket;
        private final Thread thread;

        private OneShotHttpServer(ServerSocket serverSocket, ResponseWriter writer) {
            this.serverSocket = serverSocket;
            this.thread = new Thread(() -> serveOneRequest(writer), "AlgoCraft-HttpRemoteFileClientTest");
            this.thread.setDaemon(true);
            this.thread.start();
        }

        static OneShotHttpServer start(ResponseWriter writer) throws IOException {
            ServerSocket serverSocket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
            return new OneShotHttpServer(serverSocket, writer);
        }

        URI uri() {
            return URI.create("http://" + serverSocket.getInetAddress().getHostAddress()
                    + ":" + serverSocket.getLocalPort() + "/manifest.json");
        }

        private void serveOneRequest(ResponseWriter writer) {
            try (Socket socket = serverSocket.accept()) {
                readRequestHeaders(socket.getInputStream());
                writer.write(socket.getOutputStream());
            } catch (IOException ignored) {
                // The client intentionally closes early when the size limit is exceeded.
            }
        }

        private static void readRequestHeaders(InputStream input) throws IOException {
            int matched = 0;
            byte[] delimiter = {'\r', '\n', '\r', '\n'};
            int next;
            while ((next = input.read()) != -1) {
                if (next == delimiter[matched]) {
                    matched++;
                    if (matched == delimiter.length) {
                        return;
                    }
                } else {
                    matched = next == delimiter[0] ? 1 : 0;
                }
            }
        }

        @Override
        public void close() throws Exception {
            serverSocket.close();
            thread.join(1_000);
        }
    }
}
