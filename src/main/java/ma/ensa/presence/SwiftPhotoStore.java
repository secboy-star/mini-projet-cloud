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
        if (container.getHost() == null || container.getRawUserInfo() != null
                || container.getRawQuery() != null || container.getRawFragment() != null) {
            throw new IllegalArgumentException("A Swift container URI is required");
        }
        String objectKey = UUID.randomUUID().toString();
        var request = HttpRequest.newBuilder(URI.create(container.toString().replaceFirst("/?$", "/") + objectKey))
                .timeout(Duration.ofSeconds(15)).header("X-Auth-Token", token)
                .header("Content-Type", contentType).header("If-None-Match", "*")
                .PUT(HttpRequest.BodyPublishers.ofByteArray(photo)).build();
        var response = client.send(request, HttpResponse.BodyHandlers.discarding());
        if (response.statusCode() != 201) {
            throw new IOException("Swift refused the object write (HTTP " + response.statusCode() + ")");
        }
        return objectKey;
    }
}
