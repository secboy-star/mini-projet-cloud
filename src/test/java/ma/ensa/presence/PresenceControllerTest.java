package ma.ensa.presence;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class PresenceControllerTest {
    private final MockMvc mvc = MockMvcBuilders
            .standaloneSetup(new PresenceController(new PresenceService()))
            .build();

    @Test
    void comparesRecognizedStudentsWithTheOfficialRoster() throws Exception {
        mvc.perform(post("/api/presences/preview")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"officialStudentIds":["E01","E02","E01","E03"],
                         "recognizedStudentIds":["E02","E03","E99"]}
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].studentId").value("E01"))
                .andExpect(jsonPath("$[0].status").value("A"))
                .andExpect(jsonPath("$[1].studentId").value("E02"))
                .andExpect(jsonPath("$[1].status").value("P"))
                .andExpect(jsonPath("$[2].studentId").value("E03"))
                .andExpect(jsonPath("$[2].status").value("P"));
    }

    @Test
    void rejectsMissingRecognitionInput() throws Exception {
        mvc.perform(post("/api/presences/preview")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"officialStudentIds":["E01"]}
                        """))
                .andExpect(status().isBadRequest());
    }
}
