package ma.ensa.presence;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import ma.ensa.presence.PhotoUploadOutboxRepository.PendingUpload;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

final class KafkaPhotoUploadPublisher implements PhotoUploadRelay.Publisher {
    private final Producer<String, String> producer;
    private final ObjectMapper json;

    KafkaPhotoUploadPublisher(Producer<String, String> producer, ObjectMapper json) {
        this.producer = producer;
        this.json = json;
    }

    @Override
    public void publish(PendingUpload upload) throws Exception {
        String value = json.writeValueAsString(Map.of(
                "schemaVersion", 1, "eventId", upload.eventId(), "correlationId", upload.eventId(),
                "sessionId", upload.sessionId(), "photoId", upload.swiftObjectKey(),
                "swiftObjectKey", upload.swiftObjectKey()));
        producer.send(new ProducerRecord<>("photo.uploaded", upload.sessionId().toString(), value)).get();
    }
}
