package ma.ensa.presence;

import java.util.List;
import java.util.Set;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
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

    private static boolean invalidId(String id) {
        return id == null || id.isBlank();
    }

    public record PreviewRequest(List<String> officialStudentIds, Set<String> recognizedStudentIds) {}
}
