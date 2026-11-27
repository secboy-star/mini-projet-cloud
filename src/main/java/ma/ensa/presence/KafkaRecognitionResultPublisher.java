package ma.ensa.presence;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

final class KafkaRecognitionResultPublisher {
    private static final String TOPIC = "recognition.completed";

    record Candidate(String studentId, double score) {
        Candidate {
            if (studentId == null || studentId.isBlank()) {
                throw new IllegalArgumentException("A candidate needs a student ID");
            }
            if (!Double.isFinite(score) || score < 0 || score > 1) {
                throw new IllegalArgumentException("Candidate score must be between zero and one");
            }
        }
    }

    private final Producer<String, String> producer;
    private final ObjectMapper json;

    KafkaRecognitionResultPublisher(Producer<String, String> producer, ObjectMapper json) {
        this.producer = producer;
        this.json = json;
    }

    void publish(PhotoUploadedEvent upload, String modelVersion, List<Candidate> candidates) throws Exception {
        if (modelVersion == null || modelVersion.isBlank()) {
            throw new IllegalArgumentException("A recognition result needs a model version");
        }
        // A replay of the same upload keeps the result ID for downstream deduplication.
        UUID eventId = UUID.nameUUIDFromBytes((TOPIC + ":" + upload.eventId()).getBytes(StandardCharsets.UTF_8));
        String value = json.writeValueAsString(Map.of(
                "schemaVersion", 1, "eventId", eventId, "correlationId", upload.correlationId(),
                "sessionId", upload.sessionId(), "photoId", upload.photoId(),
                "modelVersion", modelVersion, "candidates", List.copyOf(candidates)));
        producer.send(new ProducerRecord<>(TOPIC, upload.sessionId().toString(), value)).get();
    }
}
