package ma.ensa.presence;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

final class PhotoUploadOutboxRepository {
    record PendingUpload(UUID eventId, UUID sessionId, UUID swiftObjectKey) {}

    // Keep the caller's transaction open through publication and acknowledgement.
    Optional<PendingUpload> lockNext(Connection connection) throws SQLException {
        requireTransaction(connection);
        String sql = """
                SELECT event_id, session_id, swift_object_key
                FROM photo_upload_outbox
                WHERE published_at IS NULL
                ORDER BY created_at, event_id
                LIMIT 1 FOR UPDATE SKIP LOCKED
                """;
        try (var statement = connection.prepareStatement(sql);
             var rows = statement.executeQuery()) {
            if (!rows.next()) return Optional.empty();
            return Optional.of(new PendingUpload(rows.getObject("event_id", UUID.class),
                    rows.getObject("session_id", UUID.class),
                    rows.getObject("swift_object_key", UUID.class)));
        }
    }

    boolean markPublished(Connection connection, UUID eventId) throws SQLException {
        requireTransaction(connection);
        String sql = """
                UPDATE photo_upload_outbox SET published_at = clock_timestamp()
                WHERE event_id = ? AND published_at IS NULL
                """;
        try (var statement = connection.prepareStatement(sql)) {
            statement.setObject(1, eventId);
            return statement.executeUpdate() == 1;
        }
    }

    private void requireTransaction(Connection connection) throws SQLException {
        if (connection.getAutoCommit()) {
            throw new IllegalStateException("Outbox delivery requires an open transaction");
        }
    }
}
