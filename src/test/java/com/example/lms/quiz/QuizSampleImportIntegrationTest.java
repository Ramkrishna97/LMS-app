package com.example.lms.quiz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:quiz-samples-test;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class QuizSampleImportIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void everySampleMatchesTheRealQuizImportContract() throws Exception {
        Path quizDirectory = Path.of("resources", "quizzes");
        List<Path> samples;
        try (var files = Files.list(quizDirectory)) {
            samples = files.filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted().toList();
        }
        assertFalse(samples.isEmpty(), "At least one sample quiz is required");

        String login = mvc.perform(post("/api/auth/login").contentType("application/json")
                .content("{\"username\":\"asha\",\"password\":\"teacher123\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = mapper.readTree(login).get("token").asText();

        for (Path sample : samples) {
            byte[] bytes = Files.readAllBytes(sample);
            JsonNode source = mapper.readTree(bytes);
            int expectedQuestions = source.get("questions").size();
            String response = mvc.perform(multipart("/api/quizzes/import")
                    .file(new MockMultipartFile("file", sample.getFileName().toString(), "application/json", bytes))
                    .header("X-Auth-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(source.get("title").asText()))
                .andExpect(jsonPath("$.questionCount").value(expectedQuestions))
                .andReturn().getResponse().getContentAsString();
            assertEquals(expectedQuestions, mapper.readTree(response).get("questions").size(), sample.toString());
        }

        mapper.readTree(Files.readAllBytes(quizDirectory.resolve("schema").resolve("quiz.schema.json")));
    }
}
