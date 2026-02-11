package com.fiverr.demo.dto;

public class LinkResponse {
    private String shortCode;
    private String shortUrl;
    private String targetUrl;

    public LinkResponse(String shortCode, String shortUrl, String targetUrl) {
        this.shortCode = shortCode;
        this.shortUrl = shortUrl;
        this.targetUrl = targetUrl;
    }

    // Getters and Setters
    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public String getShortUrl() {
        return shortUrl;
    }

    public void setShortUrl(String shortUrl) {
        this.shortUrl = shortUrl;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }
}
