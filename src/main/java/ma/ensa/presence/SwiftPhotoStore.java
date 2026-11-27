package ma.ensa.presence;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

final class SwiftPhotoStore {
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    String upload(URI container, String token, String contentType, byte[] photo) throws IOException, InterruptedException {
        UUID objectKey = UUID.randomUUID();
        var request = objectRequest(container, token, objectKey)
                .header("Content-Type", contentType).header("If-None-Match", "*")
                .PUT(HttpRequest.BodyPublishers.ofByteArray(photo)).build();
        var response = client.send(request, HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() != 201) {
            throw new IOException("Swift refused the object write (HTTP " + response.statusCode() + ")");
        }
        return objectKey.toString();
    }

    byte[] download(URI container, String token, UUID objectKey) throws IOException, InterruptedException {
        if (objectKey == null) throw new IllegalArgumentException("A Swift object key is required");
        var request = objectRequest(container, token, objectKey).GET().build();
        var response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200) {
            throw new IOException("Swift refused the object read (HTTP " + response.statusCode() + ")");
        }
        return response.body();
    }

    void delete(URI container, String token, UUID objectKey) throws IOException, InterruptedException {
        if (objectKey == null) throw new IllegalArgumentException("A Swift object key is required");
        var request = objectRequest(container, token, objectKey).DELETE().build();
        int status = client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
        if (status != 204 && status != 404) {
            throw new IOException("Swift refused the object deletion (HTTP " + status + ")");
        }
    }

    private static HttpRequest.Builder objectRequest(URI container, String token, UUID objectKey) {
        if (container.getHost() == null || container.getRawUserInfo() != null
                || container.getRawQuery() != null || container.getRawFragment() != null) {
            throw new IllegalArgumentException("A Swift container URI is required");
        }
        URI object = URI.create(container.toString().replaceFirst("/?$", "/") + objectKey);
        return HttpRequest.newBuilder(object).timeout(Duration.ofSeconds(15)).header("X-Auth-Token", token);
    }
}
