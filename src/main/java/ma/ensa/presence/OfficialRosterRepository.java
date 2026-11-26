package ma.ensa.presence;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

final class OfficialRosterRepository {
    Optional<List<String>> findStudentIds(Connection connection, String moduleCode, int version) throws SQLException {
        String sql = """
                SELECT e.student_id FROM official_roster r
                LEFT JOIN official_roster_entry e USING (module_code, version)
                WHERE r.module_code = ? AND r.version = ? ORDER BY e.list_position
                """;
        try (var statement = connection.prepareStatement(sql)) {
            statement.setString(1, moduleCode);
            statement.setInt(2, version);
            try (var rows = statement.executeQuery()) {
                if (!rows.next()) return Optional.empty();
                var studentIds = new ArrayList<String>();
                do {
                    String id = rows.getString(1);
                    if (id != null) studentIds.add(id);
                } while (rows.next());
                return Optional.of(studentIds);
            }
        }
    }
}
