package com.example.lms.content;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:image-upload-test;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureMockMvc
class ImageUploadIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    void teacherCanUploadAndRetrieveProcessedImage() throws Exception {
        String login = mvc.perform(post("/api/auth/login").contentType("application/json")
                .content("{\"username\":\"asha\",\"password\":\"teacher123\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = mapper.readTree(login).get("token").asText();

        BufferedImage source = new BufferedImage(400, 300, BufferedImage.TYPE_INT_RGB);
        var graphics = source.createGraphics();
        graphics.setColor(Color.BLUE);
        graphics.fillRect(0, 0, 400, 300);
        graphics.dispose();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(source, "png", bytes);
        MockMultipartFile file = new MockMultipartFile("file", "lesson.png", "image/png", bytes.toByteArray());

        String uploaded = mvc.perform(multipart("/api/contents/upload").file(file)
                .header("X-Auth-Token", token)
                .param("title", "Blue lesson image").param("description", "Diagram")
                .param("type", "IMAGE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fileAvailable").value(true))
            .andExpect(jsonPath("$.fileName").value("lesson.jpg"))
            .andReturn().getResponse().getContentAsString();
        JsonNode item = mapper.readTree(uploaded);
        long id = item.get("id").asLong();

        mvc.perform(get("/api/contents/{id}/file", id))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "image/jpeg"));
        mvc.perform(get("/api/dashboard"))
            .andExpect(status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .string(org.hamcrest.Matchers.containsString("Blue lesson image")));
    }

    @Test
    void gifUploadRetainsItsFormat() throws Exception {
        String login = mvc.perform(post("/api/auth/login").contentType("application/json")
                .content("{\"username\":\"asha\",\"password\":\"teacher123\"}"))
            .andReturn().getResponse().getContentAsString();
        String token = mapper.readTree(login).get("token").asText();
        byte[] gif = Base64.getDecoder().decode("R0lGODlhAQABAAD/ACwAAAAAAQABAAACAUwAOw==");
        MockMultipartFile file = new MockMultipartFile("file", "animation.gif", "image/gif", gif);
        String uploaded = mvc.perform(multipart("/api/contents/upload").file(file)
                .header("X-Auth-Token", token)
                .param("title", "GIF lesson").param("description", "Animated diagram")
                .param("type", "IMAGE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.fileName").value("animation.gif"))
            .andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(uploaded).get("id").asLong();
        mvc.perform(get("/api/contents/{id}/file", id))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "image/gif"));
    }
}
