package com.fiverr.demo.controller;

import com.fiverr.demo.service.LinkService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RedirectController {

    private final LinkService linkService;

    public RedirectController(LinkService linkService) {
        this.linkService = linkService;
    }

    @GetMapping("/{shortCode}")
    public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
        String targetUrl = linkService.redirectAndTrack(shortCode);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Location", targetUrl);

        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}
