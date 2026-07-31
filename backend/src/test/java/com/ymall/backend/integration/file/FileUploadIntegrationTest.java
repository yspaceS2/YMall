package com.ymall.backend.integration.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class FileUploadIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 이미지 업로드 API가 Controller와 LocalFileStorageService를 통과해
     * 테스트 업로드 디렉터리에 원본 파일과 썸네일 파일을 생성하는지 검증한다.
     */
    @Test
    @DisplayName("이미지 업로드 API는 원본과 썸네일 파일을 저장한다")
    void uploadImage() throws Exception {
        MockMultipartFile file = createImageFile();

        String response = mockMvc.perform(multipart("/api/files/images").file(file))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.fileUrl").exists())
            .andExpect(jsonPath("$.data.thumbnailUrl").exists())
            .andReturn()
            .getResponse()
            .getContentAsString();

        assertThat(response).contains("/images/public/products/");
        assertThat(Files.exists(Path.of("./build/test-uploads/public/products"))).isTrue();

        String fileUrl = objectMapper.readTree(response).path("data").path("fileUrl").asText();
        mockMvc.perform(get(fileUrl))
            .andExpect(status().isOk())
            .andExpect(content().contentTypeCompatibleWith("image/jpeg"));
        mockMvc.perform(get("/images/private/refunds/secret.jpg"))
            .andExpect(status().isNotFound());
    }

    private MockMultipartFile createImageFile() throws Exception {
        BufferedImage image = new BufferedImage(600, 400, BufferedImage.TYPE_INT_RGB);
        image.getGraphics().setColor(Color.BLUE);
        image.getGraphics().fillRect(0, 0, 600, 400);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", outputStream);

        return new MockMultipartFile(
            "file",
            "product.jpg",
            "image/jpeg",
            outputStream.toByteArray()
        );
    }
}
