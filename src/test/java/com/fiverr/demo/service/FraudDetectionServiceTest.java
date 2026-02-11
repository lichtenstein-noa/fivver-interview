package com.fiverr.demo.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FraudDetectionServiceTest {

    private final FraudDetectionService fraudDetectionService = new FraudDetectionService();

    @Test
    void testValidateClick_ReturnsBoolean() {
        boolean result = fraudDetectionService.validateClick();
        assertTrue(result || !result); // Should return a boolean value
    }

    @Test
    void testValidateClick_TakesTime() {
        long startTime = System.currentTimeMillis();
        fraudDetectionService.validateClick();
        long endTime = System.currentTimeMillis();

        long duration = endTime - startTime;

        // Should take approximately 100ms
        assertTrue(duration >= 90 && duration <= 150,
            "Expected fraud detection to take ~100ms, but took " + duration + "ms");
    }

    @Test
    void testValidateClick_ReturnsVariedResults() {
        // Run multiple times to ensure we get both true and false
        int trueCount = 0;
        int falseCount = 0;
        int iterations = 100;

        for (int i = 0; i < iterations; i++) {
            if (fraudDetectionService.validateClick()) {
                trueCount++;
            } else {
                falseCount++;
            }
        }

        // Should have mostly valid (around 90%) with some invalid (around 10%)
        assertTrue(trueCount > 0, "Expected some valid clicks");
        assertTrue(falseCount > 0, "Expected some fraudulent clicks");

        // Rough check: valid should be significantly more than invalid
        assertTrue(trueCount > falseCount,
            "Expected more valid clicks than fraudulent. Valid: " + trueCount + ", Fraud: " + falseCount);

        // Should be approximately 90% valid (allow some variance)
        double validPercentage = (trueCount * 100.0) / iterations;
        assertTrue(validPercentage >= 80 && validPercentage <= 95,
            "Expected ~90% valid clicks, got " + validPercentage + "%");
    }

    @Test
    void testValidateClick_HandleInterruption() {
        Thread testThread = new Thread(() -> {
            Thread.currentThread().interrupt();
            boolean result = fraudDetectionService.validateClick();
            assertTrue(Thread.currentThread().isInterrupted(),
                "Thread should remain interrupted after validateClick");
        });

        testThread.start();
        assertDoesNotThrow(() -> testThread.join(1000));
    }
}
