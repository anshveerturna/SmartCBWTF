package com.smartcbwtf.security;

import io.github.bucket4j.Bucket;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RateLimiterTest {

    @Test
    void resolveLoginBucket_shouldCreateBucketWithCorrectLimit() {
        RateLimiterService service = new RateLimiterService();
        Bucket bucket = service.resolveLoginBucket("127.0.0.1");

        // Should allow 5 requests
        assertTrue(bucket.tryConsume(1));
        assertTrue(bucket.tryConsume(1));
        assertTrue(bucket.tryConsume(1));
        assertTrue(bucket.tryConsume(1));
        assertTrue(bucket.tryConsume(1));

        // Should block 6th request
        assertFalse(bucket.tryConsume(1));
    }

    @Test
    void resolveApiBucket_shouldCreateBucketWithCorrectLimit() {
        RateLimiterService service = new RateLimiterService();
        Bucket bucket = service.resolveApiBucket("127.0.0.1");

        // Should allow 100 requests (we won't consume all, just check it's more than 5)
        for (int i = 0; i < 10; i++) {
            assertTrue(bucket.tryConsume(1));
        }
    }
}
