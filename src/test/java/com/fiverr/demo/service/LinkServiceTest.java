package com.fiverr.demo.service;

import com.fiverr.demo.dto.LinkResponse;
import com.fiverr.demo.dto.LinkStatsDto;
import com.fiverr.demo.dto.MonthlyClickStats;
import com.fiverr.demo.entity.Click;
import com.fiverr.demo.entity.ShortenedLink;
import com.fiverr.demo.repository.ClickRepository;
import com.fiverr.demo.repository.ShortenedLinkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LinkServiceTest {

    @Mock
    private ShortenedLinkRepository linkRepository;

    @Mock
    private ClickRepository clickRepository;

    @Mock
    private FraudDetectionService fraudDetectionService;

    @InjectMocks
    private LinkService linkService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(linkService, "baseUrl", "http://localhost:8080");
    }

    @Test
    void testCreateShortLink_NewUrl() {
        // Arrange
        String targetUrl = "https://fiverr.com/seller/gig123";
        ShortenedLink savedLink = new ShortenedLink();
        savedLink.setId(1L);
        savedLink.setTargetUrl(targetUrl);
        savedLink.setShortCode("1");
        savedLink.setCreatedAt(LocalDateTime.now());

        when(linkRepository.findByTargetUrl(targetUrl)).thenReturn(Optional.empty());
        when(linkRepository.save(any(ShortenedLink.class))).thenReturn(savedLink);

        // Act
        LinkResponse response = linkService.createShortLink(targetUrl);

        // Assert
        assertNotNull(response);
        assertEquals("1", response.getShortCode());
        assertEquals("http://localhost:8080/1", response.getShortUrl());
        assertEquals(targetUrl, response.getTargetUrl());
        verify(linkRepository, times(2)).save(any(ShortenedLink.class));
    }

    @Test
    void testCreateShortLink_ExistingUrl() {
        // Arrange
        String targetUrl = "https://fiverr.com/seller/gig123";
        ShortenedLink existingLink = new ShortenedLink();
        existingLink.setId(1L);
        existingLink.setTargetUrl(targetUrl);
        existingLink.setShortCode("1");
        existingLink.setCreatedAt(LocalDateTime.now());

        when(linkRepository.findByTargetUrl(targetUrl)).thenReturn(Optional.of(existingLink));

        // Act
        LinkResponse response = linkService.createShortLink(targetUrl);

        // Assert
        assertNotNull(response);
        assertEquals("1", response.getShortCode());
        assertEquals(targetUrl, response.getTargetUrl());
        verify(linkRepository, never()).save(any());
    }

    @Test
    void testCreateShortLink_RaceCondition() {
        // Arrange
        String targetUrl = "https://fiverr.com/seller/gig123";
        ShortenedLink link = new ShortenedLink();
        link.setId(1L);
        link.setTargetUrl(targetUrl);
        link.setShortCode("1");

        when(linkRepository.findByTargetUrl(targetUrl))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.of(link));
        when(linkRepository.save(any(ShortenedLink.class)))
            .thenThrow(new DataIntegrityViolationException("Duplicate"));

        // Act
        LinkResponse response = linkService.createShortLink(targetUrl);

        // Assert
        assertNotNull(response);
        assertEquals("1", response.getShortCode());
        verify(linkRepository, times(2)).findByTargetUrl(targetUrl);
    }

    @Test
    void testRedirectAndTrack_ValidClick() {
        // Arrange
        String shortCode = "1";
        ShortenedLink link = new ShortenedLink();
        link.setId(1L);
        link.setShortCode(shortCode);
        link.setTargetUrl("https://fiverr.com/seller/gig123");

        when(linkRepository.findByShortCode(shortCode)).thenReturn(Optional.of(link));
        when(fraudDetectionService.validateClick()).thenReturn(true);
        when(clickRepository.save(any(Click.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        String targetUrl = linkService.redirectAndTrack(shortCode);

        // Assert
        assertEquals("https://fiverr.com/seller/gig123", targetUrl);
        verify(clickRepository).save(argThat(click ->
            click.getIsValid() &&
            click.getEarnings().compareTo(new BigDecimal("0.05")) == 0
        ));
    }

    @Test
    void testRedirectAndTrack_FraudDetected() {
        // Arrange
        String shortCode = "1";
        ShortenedLink link = new ShortenedLink();
        link.setId(1L);
        link.setShortCode(shortCode);
        link.setTargetUrl("https://fiverr.com/seller/gig123");

        when(linkRepository.findByShortCode(shortCode)).thenReturn(Optional.of(link));
        when(fraudDetectionService.validateClick()).thenReturn(false);
        when(clickRepository.save(any(Click.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        String targetUrl = linkService.redirectAndTrack(shortCode);

        // Assert
        assertEquals("https://fiverr.com/seller/gig123", targetUrl);
        verify(clickRepository).save(argThat(click ->
            !click.getIsValid() &&
            click.getEarnings().compareTo(BigDecimal.ZERO) == 0
        ));
    }

    @Test
    void testRedirectAndTrack_InvalidShortCode() {
        // Arrange
        String shortCode = "invalid";
        when(linkRepository.findByShortCode(shortCode)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResponseStatusException.class, () -> {
            linkService.redirectAndTrack(shortCode);
        });
        verify(clickRepository, never()).save(any());
    }

    @Test
    void testGetStats() {
        // Arrange
        ShortenedLink link1 = new ShortenedLink();
        link1.setId(1L);
        link1.setShortCode("1");
        link1.setTargetUrl("https://fiverr.com/seller/gig123");

        ShortenedLink link2 = new ShortenedLink();
        link2.setId(2L);
        link2.setShortCode("2");
        link2.setTargetUrl("https://fiverr.com/seller/gig456");

        List<ShortenedLink> links = List.of(link1, link2);
        Page<ShortenedLink> page = new PageImpl<>(links);
        Pageable pageable = PageRequest.of(0, 10);

        when(linkRepository.findAll(pageable)).thenReturn(page);
        when(clickRepository.countByLinkIdAndIsValidTrue(1L)).thenReturn(5L);
        when(clickRepository.countByLinkIdAndIsValidTrue(2L)).thenReturn(3L);
        when(clickRepository.getMonthlyStats(anyLong())).thenReturn(new ArrayList<>());

        // Act
        Page<LinkStatsDto> stats = linkService.getStats(pageable);

        // Assert
        assertNotNull(stats);
        assertEquals(2, stats.getContent().size());
        assertEquals(5L, stats.getContent().get(0).getTotalClicks());
        assertEquals(new BigDecimal("0.25"), stats.getContent().get(0).getTotalEarnings());
        assertEquals(3L, stats.getContent().get(1).getTotalClicks());
        assertEquals(new BigDecimal("0.15"), stats.getContent().get(1).getTotalEarnings());
    }
}
