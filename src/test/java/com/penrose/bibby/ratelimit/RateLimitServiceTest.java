package com.penrose.bibby.ratelimit;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RateLimitServiceTest {

  @Test
  void isAllowed_allowsInitialRequests() {
    RateLimitService service = new RateLimitService();

    assertTrue(service.isAllowed("192.168.1.1"));
  }

  @Test
  void isAllowed_blocksAfterBurstExhausted() {
    RateLimitService service = new RateLimitService();
    String ip = "10.0.0.1";

    for (int i = 0; i < 5; i++) {
      assertTrue(service.isAllowed(ip), "Request " + (i + 1) + " should be allowed");
    }
    assertFalse(service.isAllowed(ip), "Request 6 should be blocked");
  }

  @Test
  void isAllowed_tracksSeparateBucketsPerIp() {
    RateLimitService service = new RateLimitService();

    // Exhaust IP 1
    for (int i = 0; i < 5; i++) {
      service.isAllowed("ip1");
    }
    assertFalse(service.isAllowed("ip1"));

    // IP 2 should still have tokens
    assertTrue(service.isAllowed("ip2"));
  }
}
