package com.example.marketplace;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public final class InfraiClient {
    private static final String IMAGE_BATCH_CAPABILITY = "image.batch.submit";
    private final HttpClient http;
    private final String key;
    private final String baseUrl;

    public InfraiClient(String key, String baseUrl) {
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.key = key;
        this.baseUrl = baseUrl;
    }

    public String submitImageBatch(String payload) throws IOException, InterruptedException {
        if (!IMAGE_BATCH_CAPABILITY.equals("image.batch.submit")) {
            throw new IllegalStateException("image batch capability is not configured");
        }
        return request("POST", "/v1/image/batch/submit", payload);
    }

    public String consumeBuyerUpdates(String payload) throws IOException, InterruptedException {
        return request("POST", "/v1/queue/consume", payload);
    }

    private String request(String method, String path, String payload) throws IOException, InterruptedException {
        for (int attempt = 0; attempt < 4; attempt++) {
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(30)).header("Authorization", "Bearer " + key)
                    .header("Content-Type", "application/json").method(method, HttpRequest.BodyPublishers.ofString(payload)).build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();
            boolean ok = body.contains("\"ok\":true");
            if (response.statusCode() != 429) {
                if (!ok) throw new InfraiException(errorCode(body), response.statusCode());
                return body;
            }
            long retryAfter = response.headers().firstValue("Retry-After").map(Long::parseLong).orElse(1L);
            Thread.sleep(Math.max(retryAfter * 1000L, (1L << attempt) * 250L));
        }
        throw new InfraiException("RATE_LIMITED", 429);
    }

    private String errorCode(String body) {
        int start = body.indexOf("\"code\":\"");
        if (start < 0) return "REQUEST_REJECTED";
        int from = start + 8;
        int end = body.indexOf('"', from);
        return end > from ? body.substring(from, end) : "REQUEST_REJECTED";
    }
}
