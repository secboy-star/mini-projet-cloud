package ma.ensa.presence;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

final class PhotoMetadataRepository {
    void insert(Connection connection, UUID objectKey, String contentType, int byteSize) throws SQLException {
        String sql = """
                INSERT INTO attendance_photo (swift_object_key, content_type, byte_size)
                VALUES (?, ?, ?)
                """;
        try (var statement = connection.prepareStatement(sql)) {
            statement.setObject(1, objectKey);
            statement.setString(2, contentType);
            statement.setInt(3, byteSize);
            statement.executeUpdate();
        }
    }
}
