package com.fiverr.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CreateLinkRequest {
    @NotBlank(message = "Target URL is required")
    @Size(max = 2048, message = "Target URL must not exceed 2048 characters")
    private String targetUrl;

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }
}
