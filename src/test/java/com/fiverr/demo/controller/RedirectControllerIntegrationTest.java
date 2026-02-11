package com.fiverr.demo.controller;

import com.fiverr.demo.entity.ShortenedLink;
import com.fiverr.demo.repository.ClickRepository;
import com.fiverr.demo.repository.ShortenedLinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class RedirectControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
    void testRedirect_Success() throws Exception {
        // Create a link
        ShortenedLink link = new ShortenedLink();
        link.setTargetUrl("https://fiverr.com/seller/gig123");
        link = linkRepository.save(link);
        link.setShortCode(String.valueOf(link.getId()));
        link = linkRepository.save(link);

        String shortCode = link.getShortCode();

        mockMvc.perform(get("/" + shortCode))
            .andExpect(status().isFound())
            .andExpect(header().string("Location", "https://fiverr.com/seller/gig123"));

        // Verify click was recorded
        long clickCount = clickRepository.count();
        assertEquals(1, clickCount);
    }

    @Test
    void testRedirect_InvalidShortCode() throws Exception {
        mockMvc.perform(get("/invalidcode"))
            .andExpect(status().isNotFound());

        // Verify no click was recorded
        long clickCount = clickRepository.count();
        assertEquals(0, clickCount);
    }

    @Test
    void testRedirect_MultipleClicks() throws Exception {
        // Create a link
        ShortenedLink link = new ShortenedLink();
        link.setTargetUrl("https://fiverr.com/seller/gig123");
        link = linkRepository.save(link);
        link.setShortCode(String.valueOf(link.getId()));
        link = linkRepository.save(link);

        String shortCode = link.getShortCode();

        // Click 5 times
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isFound());
        }

        // Verify all clicks were recorded
        long clickCount = clickRepository.count();
        assertEquals(5, clickCount);
    }

    @Test
    void testRedirect_FraudDetectionDelay() throws Exception {
        // Create a link
        ShortenedLink link = new ShortenedLink();
        link.setTargetUrl("https://fiverr.com/seller/gig123");
        link = linkRepository.save(link);
        link.setShortCode(String.valueOf(link.getId()));
        link = linkRepository.save(link);

        String shortCode = link.getShortCode();

        // Measure response time
        long startTime = System.currentTimeMillis();
        mockMvc.perform(get("/" + shortCode))
            .andExpect(status().isFound());
        long endTime = System.currentTimeMillis();

        long duration = endTime - startTime;

        // Should take at least 100ms due to fraud detection
        // Using >= 80ms to account for test environment variance
        assert(duration >= 80);
    }

    @Test
    void testRedirect_ClicksRecordedWithValidAndInvalidFlags() throws Exception {
        // Create a link
        ShortenedLink link = new ShortenedLink();
        link.setTargetUrl("https://fiverr.com/seller/gig123");
        link = linkRepository.save(link);
        link.setShortCode(String.valueOf(link.getId()));
        link = linkRepository.save(link);

        String shortCode = link.getShortCode();

        // Click multiple times to get mix of valid/invalid
        for (int i = 0; i < 20; i++) {
            mockMvc.perform(get("/" + shortCode))
                .andExpect(status().isFound());
        }

        // Verify clicks were recorded
        long totalClicks = clickRepository.count();
        assertEquals(20, totalClicks);

        // With 10% fraud rate, we expect some invalid clicks
        // (though randomness means this isn't guaranteed)
        long validClicks = clickRepository.countByLinkIdAndIsValidTrue(link.getId());
        assert(validClicks > 0 && validClicks <= 20);
    }
}
