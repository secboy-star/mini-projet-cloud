package ma.ensa.presence;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@SpringBootApplication
public class PresenceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PresenceApplication.class, args);
    }

    @Bean
    PresenceService presenceService() {
        return new PresenceService();
    }
}

@RestController
class PresenceController {
    private final PresenceService service;
    @Value("${presence.postgres.url:}")
    private String postgresUrl;
    @Value("${presence.postgres.user:}")
    private String postgresUser;
    @Value("${presence.postgres.password:}")
    private String postgresPassword;

    public PresenceController(PresenceService service) {
        this.service = service;
    }

    @PostMapping("/api/presences/preview")
    public List<PresenceService.Row> preview(@RequestBody PreviewRequest request) {
        if (request.officialStudentIds() == null || request.recognizedStudentIds() == null
                || request.officialStudentIds().stream().anyMatch(PresenceController::invalidId)
                || request.recognizedStudentIds().stream().anyMatch(PresenceController::invalidId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid student identifiers required");
        }
        return service.calculate(request.officialStudentIds(), request.recognizedStudentIds());
    }

    @PostMapping("/api/presences/preview/official")
    public List<PresenceService.Row> previewOfficialRoster(@RequestBody RosterPreviewRequest request) {
        if (invalidId(request.moduleCode()) || request.version() < 1
                || request.recognizedStudentIds() == null
                || request.recognizedStudentIds().stream().anyMatch(PresenceController::invalidId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Valid roster and recognized IDs required");
        }
        try (var connection = DriverManager.getConnection(postgresUrl, postgresUser, postgresPassword)) {
            var roster = new OfficialRosterRepository().findStudentIds(connection, request.moduleCode(), request.version())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Official roster not found"));
            return service.calculate(roster, request.recognizedStudentIds());
        } catch (SQLException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Official roster unavailable");
        }
    }

    @PostMapping(value = "/api/presences/preview.csv", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<String> exportPreview(@RequestBody PreviewRequest request) {
        String csv = preview(request).stream()
                .map(row -> csvStudentId(row.studentId()) + "," + row.status() + "\r\n")
                .collect(Collectors.joining("", "studentId,status\r\n", ""));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=presences.csv")
                .body(csv);
    }

    private static String csvStudentId(String id) {
        String trimmed = id.stripLeading();
        // Prevent spreadsheets from interpreting an identifier as a formula.
        if ("=+-@＝＋－＠".indexOf(trimmed.charAt(0)) >= 0 || "\t\r\n".indexOf(id.charAt(0)) >= 0) {
            id = "'" + id;
        }
        return "\"" + id.replace("\"", "\"\"") + "\"";
    }

    private static boolean invalidId(String id) {
        return id == null || id.isBlank();
    }

    public record PreviewRequest(List<String> officialStudentIds, Set<String> recognizedStudentIds) {}
    public record RosterPreviewRequest(String moduleCode, int version, Set<String> recognizedStudentIds) {}
}
