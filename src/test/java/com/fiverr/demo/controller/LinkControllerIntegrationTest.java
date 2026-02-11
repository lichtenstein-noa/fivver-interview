package com.fiverr.demo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fiverr.demo.dto.CreateLinkRequest;
import com.fiverr.demo.entity.Click;
import com.fiverr.demo.entity.ShortenedLink;
import com.fiverr.demo.repository.ClickRepository;
import com.fiverr.demo.repository.ShortenedLinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:testdb",
    "spring.datasource.driver-class-name=org.h2.Driver",
    "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Transactional
class LinkControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ShortenedLinkRepository linkRepository;

    @Autowired
    private ClickRepository clickRepository;

    @BeforeEach
    void setUp() {
        clickRepository.deleteAll();
        linkRepository.deleteAll();
    }

    @Test
    void testCreateLink_Success() throws Exception {
        CreateLinkRequest request = new CreateLinkRequest();
        request.setTargetUrl("https://fiverr.com/seller/gig123");

        mockMvc.perform(post("/links")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.shortCode").exists())
            .andExpect(jsonPath("$.shortUrl").exists())
            .andExpect(jsonPath("$.targetUrl").value("https://fiverr.com/seller/gig123"));
    }

    @Test
    void testCreateLink_DuplicateUrl() throws Exception {
        CreateLinkRequest request = new CreateLinkRequest();
        request.setTargetUrl("https://fiverr.com/seller/gig123");

        // Create first link
        String response1 = mockMvc.perform(post("/links")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        // Create duplicate link
        String response2 = mockMvc.perform(post("/links")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

        // Should return same short code
        assertEquals(response1, response2);
    }

    @Test
    void testCreateLink_EmptyUrl() throws Exception {
        CreateLinkRequest request = new CreateLinkRequest();
        request.setTargetUrl("");

        mockMvc.perform(post("/links")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateLink_NullUrl() throws Exception {
        CreateLinkRequest request = new CreateLinkRequest();
        request.setTargetUrl(null);

        mockMvc.perform(post("/links")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testCreateLink_UrlTooLong() throws Exception {
        CreateLinkRequest request = new CreateLinkRequest();
        request.setTargetUrl("https://example.com/" + "a".repeat(2050));

        mockMvc.perform(post("/links")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void testGetStats_EmptyDatabase() throws Exception {
        mockMvc.perform(get("/stats")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content").isEmpty())
            .andExpect(jsonPath("$.totalElements").value(0))
            .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    void testGetStats_WithData() throws Exception {
        // Create links
        ShortenedLink link1 = new ShortenedLink();
        link1.setTargetUrl("https://fiverr.com/seller/gig1");
        link1 = linkRepository.save(link1);
        link1.setShortCode(String.valueOf(link1.getId()));
        link1 = linkRepository.save(link1);

        // Create clicks
        Click click1 = new Click();
        click1.setLink(link1);
        click1.setIsValid(true);
        click1.setEarnings(new BigDecimal("0.05"));
        clickRepository.save(click1);

        Click click2 = new Click();
        click2.setLink(link1);
        click2.setIsValid(true);
        click2.setEarnings(new BigDecimal("0.05"));
        clickRepository.save(click2);

        mockMvc.perform(get("/stats")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].totalClicks").value(2))
            .andExpect(jsonPath("$.content[0].totalEarnings").value(0.1))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void testGetStats_Pagination() throws Exception {
        // Create 15 links
        for (int i = 1; i <= 15; i++) {
            ShortenedLink link = new ShortenedLink();
            link.setTargetUrl("https://fiverr.com/seller/gig" + i);
            link = linkRepository.save(link);
            link.setShortCode(String.valueOf(link.getId()));
            linkRepository.save(link);
        }

        // Test first page
        mockMvc.perform(get("/stats")
                .param("page", "0")
                .param("size", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(5)))
            .andExpect(jsonPath("$.totalElements").value(15))
            .andExpect(jsonPath("$.totalPages").value(3))
            .andExpect(jsonPath("$.first").value(true))
            .andExpect(jsonPath("$.last").value(false));

        // Test second page
        mockMvc.perform(get("/stats")
                .param("page", "1")
                .param("size", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(5)))
            .andExpect(jsonPath("$.first").value(false))
            .andExpect(jsonPath("$.last").value(false));

        // Test last page
        mockMvc.perform(get("/stats")
                .param("page", "2")
                .param("size", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(5)))
            .andExpect(jsonPath("$.first").value(false))
            .andExpect(jsonPath("$.last").value(true));
    }

    private void assertEquals(String response1, String response2) {
        if (!response1.equals(response2)) {
            throw new AssertionError("Responses are not equal");
        }
    }
}
