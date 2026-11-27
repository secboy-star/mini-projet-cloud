package ma.ensa.presence;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

final class PhotoMetadataRepository {
    boolean lockSession(Connection connection, UUID sessionId) throws SQLException {
        try (var statement = connection.prepareStatement(
                "SELECT 1 FROM attendance_session WHERE session_id = ? FOR KEY SHARE")) {
            statement.setObject(1, sessionId);
            try (var rows = statement.executeQuery()) {
                return rows.next();
            }
        }
    }

    void insert(Connection connection, UUID objectKey, String contentType, int byteSize, UUID sessionId) throws SQLException {
        String sql = """
                INSERT INTO attendance_photo (swift_object_key, content_type, byte_size, session_id)
                VALUES (?, ?, ?, ?)
                """;
        try (var statement = connection.prepareStatement(sql)) {
            statement.setObject(1, objectKey);
            statement.setString(2, contentType);
            statement.setInt(3, byteSize);
            statement.setObject(4, sessionId);
            statement.executeUpdate();
        }
    }
}
