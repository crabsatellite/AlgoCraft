package com.crabmods.algocraft.logic.repo;

import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

final class HttpRemoteFileClient implements RemoteFileClient {
    private final HttpClient httpClient;

    HttpRemoteFileClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    HttpRemoteFileClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    @Override
    public byte[] get(URI uri) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        try (InputStream body = response.body()) {
            if (response.statusCode() != 200) {
                throw new IOException("HTTP " + response.statusCode() + " for " + uri);
            }
            long declaredLength = response.headers().firstValueAsLong("content-length").orElse(-1L);
            if (declaredLength > RemoteRepositoryDownloader.MAX_REPOSITORY_FILE_BYTES) {
                throw new IOException("HTTP body exceeds maximum size of "
                        + RemoteRepositoryDownloader.MAX_REPOSITORY_FILE_BYTES + " bytes for " + uri);
            }
            return readBodyWithLimit(body, uri);
        }
    }

    private static byte[] readBodyWithLimit(InputStream body, URI uri) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        long total = 0L;
        int read;
        while ((read = body.read(buffer)) != -1) {
            total += read;
            if (total > RemoteRepositoryDownloader.MAX_REPOSITORY_FILE_BYTES) {
                throw new IOException("HTTP body exceeds maximum size of "
                        + RemoteRepositoryDownloader.MAX_REPOSITORY_FILE_BYTES + " bytes for " + uri);
            }
            output.write(buffer, 0, read);
        }
        return output.toByteArray();
    }
}
