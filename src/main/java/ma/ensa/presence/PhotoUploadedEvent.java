package ma.ensa.presence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.apache.kafka.clients.consumer.ConsumerRecord;

record PhotoUploadedEvent(UUID eventId, UUID correlationId, UUID sessionId, UUID photoId, UUID swiftObjectKey) {
    static PhotoUploadedEvent decode(ConsumerRecord<String, String> record, ObjectMapper json)
            throws JsonProcessingException {
        if (!"photo.uploaded".equals(record.topic()) || record.value() == null) {
            throw new IllegalArgumentException("A photo.uploaded message is required");
        }
        JsonNode body = json.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS,
                DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY).readTree(record.value());
        JsonNode version = body.path("schemaVersion");
        if (!version.isIntegralNumber() || !version.canConvertToInt() || version.intValue() != 1) {
            throw new IllegalArgumentException("Unsupported photo.uploaded schema version");
        }
        UUID sessionId = readId(body, "sessionId");
        if (!sessionId.toString().equals(record.key())) {
            throw new IllegalArgumentException("Photo event key must match its session ID");
        }
        return new PhotoUploadedEvent(readId(body, "eventId"), readId(body, "correlationId"), sessionId,
                readId(body, "photoId"), readId(body, "swiftObjectKey"));
    }

    private static UUID readId(JsonNode body, String field) {
        String value = body.path(field).textValue();
        if (value == null || !value.matches("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")) {
            throw new IllegalArgumentException("Invalid photo event field: " + field);
        }
        return UUID.fromString(value);
    }
}
