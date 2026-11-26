package ma.ensa.presence;

import java.util.List;
import java.util.Set;

public class PresenceService {
    public List<Row> calculate(List<String> roster, Set<String> recognized) {
        return roster.stream()
                .distinct()
                .map(id -> new Row(id, recognized.contains(id) ? Status.P : Status.A))
                .toList();
    }

    public record Row(String studentId, Status status) {}
    public enum Status { P, A }
}
