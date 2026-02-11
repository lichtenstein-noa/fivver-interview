package com.fiverr.demo.service;

import com.fiverr.demo.dto.LinkResponse;
import com.fiverr.demo.dto.LinkStatsDto;
import com.fiverr.demo.dto.MonthlyClickStats;
import com.fiverr.demo.entity.Click;
import com.fiverr.demo.entity.ShortenedLink;
import com.fiverr.demo.repository.ClickRepository;
import com.fiverr.demo.repository.ShortenedLinkRepository;
import com.fiverr.demo.util.Base62Encoder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class LinkService {

    private final ShortenedLinkRepository linkRepository;
    private final ClickRepository clickRepository;
    private final FraudDetectionService fraudDetectionService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public LinkService(ShortenedLinkRepository linkRepository,
                      ClickRepository clickRepository,
                      FraudDetectionService fraudDetectionService) {
        this.linkRepository = linkRepository;
        this.clickRepository = clickRepository;
        this.fraudDetectionService = fraudDetectionService;
    }

    @Transactional
    public LinkResponse createShortLink(String targetUrl) {
        // Check for existing
        Optional<ShortenedLink> existing = linkRepository.findByTargetUrl(targetUrl);
        if (existing.isPresent()) {
            return toResponse(existing.get());
        }

        // Create new
        try {
            ShortenedLink link = new ShortenedLink();
            link.setTargetUrl(targetUrl);
            link = linkRepository.save(link); // Get ID

            link.setShortCode(Base62Encoder.encode(link.getId()));
            link = linkRepository.save(link);
            return toResponse(link);
        } catch (DataIntegrityViolationException e) {
            // Race condition: another thread created it
            return toResponse(linkRepository.findByTargetUrl(targetUrl).orElseThrow());
        }
    }

    @Transactional
    public String redirectAndTrack(String shortCode) {
        ShortenedLink link = linkRepository.findByShortCode(shortCode)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Short link not found"));

        boolean isValid = fraudDetectionService.validateClick(); // 100ms delay

        Click click = new Click();
        click.setLink(link);
        click.setIsValid(isValid);
        click.setEarnings(isValid ? new BigDecimal("0.05") : BigDecimal.ZERO);
        clickRepository.save(click);

        return link.getTargetUrl();
    }

    @Transactional(readOnly = true)
    public Page<LinkStatsDto> getStats(Pageable pageable) {
        Page<ShortenedLink> links = linkRepository.findAll(pageable);

        return links.map(link -> {
            long totalClicks = clickRepository.countByLinkIdAndIsValidTrue(link.getId());
            BigDecimal totalEarnings = new BigDecimal("0.05").multiply(new BigDecimal(totalClicks));

            List<MonthlyClickStats> monthlyStats = clickRepository.getMonthlyStats(link.getId());
            Map<String, Long> monthlyBreakdown = new LinkedHashMap<>();
            for (MonthlyClickStats stat : monthlyStats) {
                monthlyBreakdown.put(stat.getMonth(), stat.getClickCount());
            }

            return new LinkStatsDto(
                link.getShortCode(),
                link.getTargetUrl(),
                totalClicks,
                totalEarnings,
                monthlyBreakdown
            );
        });
    }

    private LinkResponse toResponse(ShortenedLink link) {
        String shortUrl = baseUrl + "/" + link.getShortCode();
        return new LinkResponse(link.getShortCode(), shortUrl, link.getTargetUrl());
    }
}
