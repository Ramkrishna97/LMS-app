package com.example.lms;

import com.example.lms.auth.AuthSessionRepository;
import com.example.lms.auth.AuthService;
import com.example.lms.user.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:features-test;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "app.cors.allowed-origins=https://*.netlify.app"
})
@AutoConfigureMockMvc
class PlatformFeaturesIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired AuthSessionRepository sessions;
    @Autowired UserRepository users;

    private String login(String username, String password) throws Exception {
        String response = mvc.perform(post("/api/auth/login").contentType("application/json")
                .content(mapper.writeValueAsString(java.util.Map.of("username", username, "password", password))))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(response).get("token").asText();
    }

    private String student() throws Exception {
        String name = "student" + UUID.randomUUID().toString().substring(0, 8);
        mvc.perform(post("/api/auth/register").contentType("application/json")
                .content(mapper.writeValueAsString(java.util.Map.of("name", name, "email", name + "@example.test",
                        "username", name, "password", "student-pass", "role", "STUDENT"))))
            .andExpect(status().isOk());
        return login(name, "student-pass");
    }

    private String quizJson(String title) {
        return """
            {"title":"%s","description":"A quiz","durationMinutes":15,
             "questions":[{"questionText":"Choose A","optionA":"A","optionB":"B","optionC":"C",
                           "optionD":"D","correctOption":0,"solution":"A is correct"}]}
            """.formatted(title);
    }

    @Test
    void teacherCanPublishAllTypesAndSessionIsStored() throws Exception {
        String teacher = login("asha", "teacher123");
        org.junit.jupiter.api.Assertions.assertTrue(sessions.existsById(teacher));
        org.junit.jupiter.api.Assertions.assertEquals("asha", new AuthService(users, sessions).current(teacher).getUsername());
        mvc.perform(get("/api/auth/me").header("X-Auth-Token", teacher)).andExpect(status().isOk());
        mvc.perform(post("/api/contents").header("X-Auth-Token", teacher).contentType("application/json")
                .content("{\"title\":\"Teacher post\",\"body\":\"Lesson\",\"type\":\"POST\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.author.role").value("TEACHER"));
        mvc.perform(multipart("/api/contents/upload")
                .file(new MockMultipartFile("file", "notes.txt", "text/plain", "Notes".getBytes(StandardCharsets.UTF_8)))
                .header("X-Auth-Token", teacher).param("title", "Teacher notes")
                .param("description", "Notes for class").param("type", "DOCUMENT"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.fileAvailable").value(true));
        byte[] png = java.util.Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+/+1sAAAAASUVORK5CYII=");
        mvc.perform(multipart("/api/contents/upload")
                .file(new MockMultipartFile("file", "picture.png", "image/png", png))
                .header("X-Auth-Token", teacher).param("title", "Teacher picture")
                .param("description", "Class image").param("type", "IMAGE"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.fileAvailable").value(true));
        mvc.perform(post("/api/quizzes").header("X-Auth-Token", teacher).contentType("application/json")
                .content(quizJson("Manual quiz")))
            .andExpect(status().isOk()).andExpect(jsonPath("$.questionCount").value(1));
        mvc.perform(multipart("/api/quizzes/import")
                .file(new MockMultipartFile("file", "quiz.json", "application/json",
                        quizJson("Imported quiz").getBytes(StandardCharsets.UTF_8)))
                .header("X-Auth-Token", teacher))
            .andExpect(status().isOk());
        mvc.perform(get("/api/dashboard"))
            .andExpect(status().isOk()).andExpect(content().string(containsString("Teacher notes")))
            .andExpect(content().string(containsString("Manual quiz")));
    }

    @Test
    void countsUniqueStudentsAndAdminCanFilterAndRemoveQuiz() throws Exception {
        String teacher = login("asha", "teacher123");
        String admin = login("admin", "qwertyuiop");
        JsonNode teacherUser = mapper.readTree(mvc.perform(get("/api/auth/me").header("X-Auth-Token", teacher))
                .andReturn().getResponse().getContentAsString());
        String first = student();
        String second = student();
        String title = "Count quiz " + UUID.randomUUID();
        JsonNode created = mapper.readTree(mvc.perform(post("/api/quizzes").header("X-Auth-Token", teacher)
                .contentType("application/json").content(quizJson(title)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        long quizId = created.get("id").asLong();
        long contentId = created.get("contentId").asLong();
        long questionId = created.get("questions").get(0).get("id").asLong();
        String answers = "{\"answers\":{\"" + questionId + "\":0}}";
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/quizzes/{id}/attempt", quizId).header("X-Auth-Token", first)
                    .contentType("application/json").content(answers)).andExpect(status().isOk())
                .andExpect(jsonPath("$.attemptCount").value(1))
                .andExpect(jsonPath("$.maxScore").value(1));
        }
        mvc.perform(get("/api/quizzes/content/{id}", contentId))
            .andExpect(jsonPath("$.attemptCount").value(1));
        mvc.perform(post("/api/quizzes/{id}/attempt", quizId).header("X-Auth-Token", second)
                .contentType("application/json").content(answers)).andExpect(status().isOk())
            .andExpect(jsonPath("$.attemptCount").value(2));
        mvc.perform(get("/api/quizzes/content/{id}", contentId))
            .andExpect(jsonPath("$.attemptCount").value(2));
        mvc.perform(get("/api/dashboard"))
            .andExpect(content().string(containsString("\"quizAttemptCount\":2")));
        mvc.perform(get("/api/contents/manage").header("X-Auth-Token", admin)
                .param("type", "QUIZ").param("authorId", teacherUser.get("id").asText())
                .param("q", title).param("from", "2000-01-01").param("to", "2999-12-31"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].title").value(title));
        mvc.perform(get("/api/contents/manage").header("X-Auth-Token", admin)
                .param("q", title).param("to", "2000-01-01"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(0));
        mvc.perform(delete("/api/contents/{id}", contentId).header("X-Auth-Token", admin))
            .andExpect(status().isOk());
        mvc.perform(get("/api/contents/{id}", contentId)).andExpect(status().isNotFound());
    }

    @Test
    void invalidQuizPayloadsDoNotLeavePublishedContent() throws Exception {
        String teacher = login("asha", "teacher123");
        String title = "Invalid quiz " + UUID.randomUUID();
        String invalid = quizJson(title).replace("\"durationMinutes\":15", "\"durationMinutes\":0");
        mvc.perform(post("/api/quizzes").header("X-Auth-Token", teacher).contentType("application/json")
                .content(invalid)).andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/quizzes/import")
                .file(new MockMultipartFile("file", "invalid.json", "application/json",
                        quizJson(title).replace("\"description\":\"A quiz\",", "")
                            .getBytes(StandardCharsets.UTF_8)))
                .header("X-Auth-Token", teacher)).andExpect(status().isBadRequest());
        mvc.perform(get("/api/dashboard")).andExpect(content().string(not(containsString(title))));
    }

    @Test
    void teacherCanManageOnlyTheirOwnPublishedContent() throws Exception {
        String firstTeacher = login("asha", "teacher123");
        String teacherName = "teacher" + UUID.randomUUID().toString().substring(0, 8);
        mvc.perform(post("/api/auth/register").contentType("application/json")
                .content(mapper.writeValueAsString(java.util.Map.of("name", teacherName,
                        "email", teacherName + "@example.test", "username", teacherName,
                        "password", "teacher-pass", "role", "TEACHER"))))
            .andExpect(status().isOk());
        String secondTeacher = login(teacherName, "teacher-pass");
        String marker = "Owned content " + UUID.randomUUID();
        JsonNode owned = mapper.readTree(mvc.perform(post("/api/contents").header("X-Auth-Token", firstTeacher)
                .contentType("application/json").content(mapper.writeValueAsString(java.util.Map.of(
                        "title", marker + " A", "body", "Original", "type", "POST"))))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        JsonNode foreign = mapper.readTree(mvc.perform(post("/api/contents").header("X-Auth-Token", secondTeacher)
                .contentType("application/json").content(mapper.writeValueAsString(java.util.Map.of(
                        "title", marker + " B", "body", "Other", "type", "POST"))))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());

        mvc.perform(get("/api/contents/mine").header("X-Auth-Token", firstTeacher).param("q", marker))
            .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].id").value(owned.get("id").asLong()))
            .andExpect(jsonPath("$.content[0].author.username").value("asha"));
        mvc.perform(get("/api/contents/mine").header("X-Auth-Token", student()))
            .andExpect(status().isBadRequest());
        mvc.perform(put("/api/contents/{id}", foreign.get("id").asLong()).header("X-Auth-Token", firstTeacher)
                .contentType("application/json").content("{\"title\":\"Not allowed\",\"body\":\"No\",\"type\":\"POST\"}"))
            .andExpect(status().isBadRequest());
        mvc.perform(put("/api/contents/{id}", owned.get("id").asLong()).header("X-Auth-Token", firstTeacher)
                .contentType("application/json").content("{\"title\":\"Updated own content\",\"body\":\"Changed\",\"type\":\"POST\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("Updated own content"));
        mvc.perform(delete("/api/contents/{id}", owned.get("id").asLong()).header("X-Auth-Token", firstTeacher))
            .andExpect(status().isOk());
        mvc.perform(get("/api/contents/{id}", owned.get("id").asLong())).andExpect(status().isNotFound());
    }

    @Test
    void adminCanRemoveTeacherWithContentAndSessions() throws Exception {
        String name = "teacher" + UUID.randomUUID().toString().substring(0, 8);
        JsonNode user = mapper.readTree(mvc.perform(post("/api/auth/register").contentType("application/json")
                .content(mapper.writeValueAsString(java.util.Map.of("name", name, "email", name + "@example.test",
                        "username", name, "password", "teacher-pass", "role", "TEACHER"))))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        String token = login(name, "teacher-pass");
        String admin = login("admin", "qwertyuiop");
        JsonNode post = mapper.readTree(mvc.perform(post("/api/contents").header("X-Auth-Token", token)
                .contentType("application/json").content("{\"title\":\"Teacher deletion post\",\"body\":\"Text\",\"type\":\"POST\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        JsonNode quiz = mapper.readTree(mvc.perform(post("/api/quizzes").header("X-Auth-Token", token)
                .contentType("application/json").content(quizJson("Teacher deletion quiz")))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        mvc.perform(post("/api/quizzes/{id}/attempt", quiz.get("id").asLong())
                .header("X-Auth-Token", student()).contentType("application/json")
                .content("{\"answers\":{}}"))
            .andExpect(status().isOk());
        mvc.perform(delete("/api/users/{id}", user.get("id").asLong()).header("X-Auth-Token", admin))
            .andExpect(status().isOk());
        mvc.perform(get("/api/contents/{id}", post.get("id").asLong())).andExpect(status().isNotFound());
        mvc.perform(get("/api/contents/{id}", quiz.get("contentId").asLong())).andExpect(status().isNotFound());
        mvc.perform(get("/api/auth/me").header("X-Auth-Token", token)).andExpect(status().isBadRequest());
    }

    @Test
    void deploymentHealthAndNetlifyCorsAreAvailable() throws Exception {
        mvc.perform(get("/api/health"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ok"));
        mvc.perform(options("/api/dashboard")
                .header("Origin", "https://learnhub-preview.netlify.app")
                .header("Access-Control-Request-Method", "GET"))
            .andExpect(status().isOk())
            .andExpect(header().string("Access-Control-Allow-Origin", "https://learnhub-preview.netlify.app"));
    }
}
