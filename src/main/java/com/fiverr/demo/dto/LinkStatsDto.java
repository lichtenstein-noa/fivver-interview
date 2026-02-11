package com.fiverr.demo.dto;

import java.math.BigDecimal;
import java.util.Map;

public class LinkStatsDto {
    private String shortCode;
    private String targetUrl;
    private long totalClicks;
    private BigDecimal totalEarnings;
    private Map<String, Long> monthlyBreakdown;

    public LinkStatsDto(String shortCode, String targetUrl, long totalClicks,
                        BigDecimal totalEarnings, Map<String, Long> monthlyBreakdown) {
        this.shortCode = shortCode;
        this.targetUrl = targetUrl;
        this.totalClicks = totalClicks;
        this.totalEarnings = totalEarnings;
        this.monthlyBreakdown = monthlyBreakdown;
    }

    // Getters and Setters
    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public long getTotalClicks() {
        return totalClicks;
    }

    public void setTotalClicks(long totalClicks) {
        this.totalClicks = totalClicks;
    }

    public BigDecimal getTotalEarnings() {
        return totalEarnings;
    }

    public void setTotalEarnings(BigDecimal totalEarnings) {
        this.totalEarnings = totalEarnings;
    }

    public Map<String, Long> getMonthlyBreakdown() {
        return monthlyBreakdown;
    }

    public void setMonthlyBreakdown(Map<String, Long> monthlyBreakdown) {
        this.monthlyBreakdown = monthlyBreakdown;
    }
}
