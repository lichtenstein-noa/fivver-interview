package com.fiverr.demo.service;

import org.springframework.stereotype.Service;

import java.util.Random;

@Service
public class FraudDetectionService {
    private final Random random = new Random();

    public boolean validateClick() {
        try {
            // Simulate fraud detection delay
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        // Randomly mark 10% of clicks as fraudulent
        return random.nextInt(10) != 0;
    }
}
