package ma.ensa.presence;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.HashMap;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

// Use a subscribed consumer with enable.auto.commit=false.
final class PhotoUploadConsumer {
    @FunctionalInterface
    interface Handler {
        // Processing must be safe to repeat if a batch is replayed.
        void process(PhotoUploadedEvent event) throws Exception;
    }

    @FunctionalInterface
    private interface RecordHandler {
        void process(ConsumerRecord<String, String> record) throws Exception;
    }

    private final Consumer<String, String> consumer;
    private final ObjectMapper json;

    PhotoUploadConsumer(Consumer<String, String> consumer, ObjectMapper json) {
        this.consumer = consumer;
        this.json = json;
    }

    int pollOnce(Duration timeout, Handler handler) throws Exception {
        return pollRecords(consumer, timeout, record -> handler.process(PhotoUploadedEvent.decode(record, json)));
    }

    static int pollRecognitionResults(Consumer<String, String> consumer, Duration timeout,
            RecognitionResultHandler handler) throws Exception {
        return pollRecords(consumer, timeout, handler::process);
    }

    private static int pollRecords(Consumer<String, String> consumer, Duration timeout, RecordHandler handler)
            throws Exception {
        var records = consumer.poll(timeout);
        if (records.isEmpty()) return 0;

        var offsets = new HashMap<TopicPartition, OffsetAndMetadata>();
        try {
            for (var record : records) {
                handler.process(record);
                offsets.put(new TopicPartition(record.topic(), record.partition()),
                        new OffsetAndMetadata(record.offset() + 1));
            }
            consumer.commitSync(offsets);
            return records.count();
        } catch (Exception failure) {
            try {
                var assigned = consumer.assignment();
                for (var partition : records.partitions()) {
                    if (assigned.contains(partition)) {
                        consumer.seek(partition, records.records(partition).get(0).offset());
                    }
                }
            } catch (RuntimeException rewindFailure) {
                failure.addSuppressed(rewindFailure);
            }
            throw failure;
        }
    }
}
