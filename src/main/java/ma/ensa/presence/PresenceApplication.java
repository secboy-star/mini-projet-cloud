package ma.ensa.presence;

import java.io.IOException;
import java.net.URI;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HexFormat;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
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
    private final SwiftPhotoStore photoStore = new SwiftPhotoStore();
    @Value("${presence.swift.container-url:}")
    private String swiftContainer;
    @Value("${presence.swift.token:}")
    private String swiftToken;
    @Value("${presence.postgres.url:}")
    private String postgresUrl;
    @Value("${presence.postgres.user:}")
    private String postgresUser;
    @Value("${presence.postgres.password:}")
    private String postgresPassword;

    public PresenceController(PresenceService service) {
        this.service = service;
    }

    @PostMapping(value = "/api/photos", consumes = "multipart/form-data", produces = "text/plain")
    public ResponseEntity<String> uploadPhoto(@RequestParam("file") MultipartFile file) {
        if (swiftContainer == null || swiftContainer.isBlank() || swiftToken == null || swiftToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Photo storage unavailable");
        }
        try {
            byte[] photo = file.getBytes();
            if (photo.length < 8 || !HexFormat.of().formatHex(photo, 0, 8).equals("89504e470d0a1a0a")) {
                throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "PNG photo required");
            }
            String key = photoStore.upload(URI.create(swiftContainer), swiftToken, "image/png", photo);
            return ResponseEntity.status(HttpStatus.CREATED).body(key);
        } catch (IOException | InterruptedException | IllegalArgumentException failure) {
            if (failure instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Photo storage unavailable");
        }
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
        return csvResponse(preview(request));
    }

    @PostMapping(value = "/api/presences/preview/official.csv", produces = "text/csv;charset=UTF-8")
    public ResponseEntity<String> exportOfficialPreview(@RequestBody RosterPreviewRequest request) {
        return csvResponse(previewOfficialRoster(request));
    }

    private static ResponseEntity<String> csvResponse(List<PresenceService.Row> rows) {
        String csv = rows.stream()
                .map(row -> csvStudentId(row.studentId()) + "," + row.status() + "\r\n")
                .collect(Collectors.joining("", "studentId,status\r\n", ""));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=presences.csv")
                .body(csv);
    }

    private static String csvStudentId(String id) {
        String trimmed = id.stripLeading();
        // Prevent spreadsheets from interpreting an identifier as a formula.
        boolean startsWithFormula = !trimmed.isEmpty() && "=+-@＝＋－＠".indexOf(trimmed.charAt(0)) >= 0;
        if (startsWithFormula || (!id.isEmpty() && "\t\r\n".indexOf(id.charAt(0)) >= 0)) {
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
