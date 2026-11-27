package ma.ensa.presence;

import java.sql.Connection;
import java.sql.SQLException;
import ma.ensa.presence.PhotoUploadOutboxRepository.PendingUpload;

final class PhotoUploadRelay {
    @FunctionalInterface
    interface Publisher {
        // Return only after the broker acknowledges receipt.
        void publish(PendingUpload upload) throws Exception;
    }

    private final PhotoUploadOutboxRepository outbox = new PhotoUploadOutboxRepository();

    // The caller must commit on success and roll back on failure.
    boolean publishNext(Connection connection, Publisher publisher) throws Exception {
        var pending = outbox.lockNext(connection);
        if (pending.isEmpty()) {
            return false;
        }
        var upload = pending.orElseThrow();
        publisher.publish(upload);
        if (!outbox.markPublished(connection, upload.eventId())) {
            throw new SQLException("Could not acknowledge the locked photo upload event");
        }
        return true;
    }
}
