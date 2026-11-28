package ma.ensa.presence;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.postgresql.ds.PGSimpleDataSource;
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

    @Bean(destroyMethod = "close")
    AutoCloseable recognitionResultPolling(Consumer<String, String> recognitionResultConsumer, ObjectMapper json,
            @Value("${presence.postgres.url}") String url, @Value("${presence.postgres.user:}") String user,
            @Value("${presence.postgres.password:}") String password) {
        var dataSource = new PGSimpleDataSource();
        dataSource.setURL(url);
        dataSource.setUser(user);
        dataSource.setPassword(password);
        dataSource.setSocketTimeout(5);
        var handler = new RecognitionResultHandler(dataSource, json);
        var scheduler = Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleWithFixedDelay(() -> {
            try {
                PhotoUploadConsumer.pollRecognitionResults(recognitionResultConsumer, Duration.ofMillis(500), handler);
            } catch (Exception failure) {
                System.getLogger(getClass().getName()).log(System.Logger.Level.WARNING,
                        "Recognition result intake failed ({0})", failure.getClass().getSimpleName());
            }
        }, 0, 1, TimeUnit.SECONDS);
        return () -> {
            scheduler.shutdown();
            if (!scheduler.awaitTermination(10, TimeUnit.SECONDS)) scheduler.shutdownNow();
        };
    }
}
