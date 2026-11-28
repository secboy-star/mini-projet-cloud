package ma.ensa.presence;

import java.util.List;
import java.util.Map;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "presence.kafka.recognition-results-enabled", havingValue = "true")
class RecognitionResultKafkaConfiguration {
    @Bean(destroyMethod = "close")
    Consumer<String, String> recognitionResultConsumer(
            @Value("${presence.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${presence.kafka.recognition-results-group:presence-recognition-results}") String group) {
        var consumer = new KafkaConsumer<>(Map.of(
                "bootstrap.servers", bootstrapServers,
                "group.id", group,
                "enable.auto.commit", false, "auto.offset.reset", "earliest",
                "default.api.timeout.ms", 5000, "max.poll.records", 10),
                new StringDeserializer(), new StringDeserializer());
        consumer.subscribe(List.of("recognition.completed"));
        return consumer;
    }
}
