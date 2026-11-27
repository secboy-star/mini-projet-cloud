package ma.ensa.presence;

import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
@ConditionalOnProperty(name = "presence.kafka.enabled", havingValue = "true")
class PhotoUploadWorker {
    private final PhotoUploadRelay relay = new PhotoUploadRelay();
    private final PGSimpleDataSource dataSource = new PGSimpleDataSource();
    private final KafkaPhotoUploadPublisher publisher;

    PhotoUploadWorker(KafkaPhotoUploadPublisher publisher, @Value("${presence.postgres.url}") String url,
            @Value("${presence.postgres.user:}") String user, @Value("${presence.postgres.password:}") String password) {
        dataSource.setURL(url);
        dataSource.setUser(user);
        dataSource.setPassword(password);
        this.publisher = publisher;
    }
    @Scheduled(fixedDelayString = "${presence.kafka.poll-delay-ms:1000}")
    void publishPendingUpload() throws Exception {
        relay.publishNextInTransaction(dataSource, publisher);
    }
}
