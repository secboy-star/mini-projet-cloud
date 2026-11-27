package ma.ensa.presence;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "presence.kafka.enabled", havingValue = "true")
class PhotoUploadKafkaConfiguration {
    @Bean(destroyMethod = "close")
    Producer<String, String> photoUploadProducer(
            @Value("${presence.kafka.bootstrap-servers}") String bootstrapServers) {
        return new KafkaProducer<>(Map.of(
                "bootstrap.servers", bootstrapServers,
                "acks", "all", "max.block.ms", 5000, "delivery.timeout.ms", 15000,
                "request.timeout.ms", 5000), new StringSerializer(), new StringSerializer());
    }

    @Bean
    KafkaPhotoUploadPublisher photoUploadPublisher(Producer<String, String> producer, ObjectMapper json) {
        return new KafkaPhotoUploadPublisher(producer, json);
    }
}
