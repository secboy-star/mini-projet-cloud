package ma.ensa.presence;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import ma.ensa.presence.KafkaRecognitionResultPublisher.Candidate;

final class RecognitionResultRepository {
    boolean save(DataSource dataSource, UUID eventId, UUID correlationId, UUID sessionId, UUID photoId,
            String modelVersion, List<Candidate> candidates) throws SQLException {
        try (var connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                boolean inserted = insertResult(connection, eventId, correlationId, sessionId, photoId, modelVersion);
                if (inserted) insertCandidates(connection, eventId, candidates);
                connection.commit();
                return inserted;
            } catch (SQLException | RuntimeException failure) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    failure.addSuppressed(rollbackFailure);
                }
                throw failure;
            }
        }
    }

    private boolean insertResult(Connection connection, UUID eventId, UUID correlationId, UUID sessionId,
            UUID photoId, String modelVersion) throws SQLException {
        String sql = """
                INSERT INTO recognition_result (event_id, correlation_id, session_id, photo_id, model_version)
                VALUES (?, ?, ?, ?, ?) ON CONFLICT (event_id) DO NOTHING
                """;
        try (var statement = connection.prepareStatement(sql)) {
            statement.setObject(1, eventId);
            statement.setObject(2, correlationId);
            statement.setObject(3, sessionId);
            statement.setObject(4, photoId);
            statement.setString(5, modelVersion);
            return statement.executeUpdate() == 1;
        }
    }

    private void insertCandidates(Connection connection, UUID eventId, List<Candidate> candidates) throws SQLException {
        String sql = """
                INSERT INTO recognition_candidate (event_id, candidate_position, student_id, score)
                VALUES (?, ?, ?, ?)
                """;
        try (var statement = connection.prepareStatement(sql)) {
            int position = 1;
            for (var candidate : candidates) {
                statement.setObject(1, eventId);
                statement.setInt(2, position++);
                statement.setString(3, candidate.studentId());
                statement.setDouble(4, candidate.score());
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }
}
