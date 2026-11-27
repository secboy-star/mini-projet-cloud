package ma.ensa.presence;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;
import javax.sql.DataSource;
import ma.ensa.presence.KafkaRecognitionResultPublisher.Candidate;
import org.apache.kafka.clients.consumer.ConsumerRecord;

final class RecognitionResultHandler {
    private final RecognitionResultRepository repository = new RecognitionResultRepository();
    private final DataSource dataSource;
    private final ObjectMapper json;

    RecognitionResultHandler(DataSource dataSource, ObjectMapper json) {
        this.dataSource = dataSource;
        this.json = json;
    }

    boolean process(ConsumerRecord<String, String> record) throws IOException, SQLException {
        if (!"recognition.completed".equals(record.topic()) || record.value() == null) {
            throw new IllegalArgumentException("A recognition.completed message is required");
        }
        JsonNode body = json.reader().with(DeserializationFeature.FAIL_ON_TRAILING_TOKENS,
                DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY).readTree(record.value());
        JsonNode version = body.path("schemaVersion");
        if (!version.isIntegralNumber() || !version.canConvertToInt() || version.intValue() != 1) {
            throw new IllegalArgumentException("Unsupported recognition schema version");
        }
        UUID sessionId = readId(body, "sessionId");
        if (!sessionId.toString().equals(record.key())) {
            throw new IllegalArgumentException("Recognition event key must match its session ID");
        }
        JsonNode candidates = body.path("candidates");
        if (!candidates.isArray()) throw new IllegalArgumentException("A candidate array is required");
        var parsed = new ArrayList<Candidate>();
        for (JsonNode candidate : candidates) {
            JsonNode score = candidate.path("score");
            if (!score.isNumber()) throw new IllegalArgumentException("Candidate score must be numeric");
            parsed.add(new Candidate(readText(candidate, "studentId"), score.doubleValue()));
        }
        return repository.save(dataSource, readId(body, "eventId"), readId(body, "correlationId"),
                sessionId, readId(body, "photoId"), readText(body, "modelVersion"), parsed);
    }

    private static UUID readId(JsonNode body, String field) {
        String value = readText(body, field);
        if (!value.matches("[0-9a-fA-F]{8}(-[0-9a-fA-F]{4}){3}-[0-9a-fA-F]{12}")) {
            throw new IllegalArgumentException("Invalid recognition ID: " + field);
        }
        return UUID.fromString(value);
    }

    private static String readText(JsonNode body, String field) {
        String value = body.path(field).textValue();
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing recognition field: " + field);
        return value;
    }
}
