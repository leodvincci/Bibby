package com.penrose.bibby.ratelimit.tokenbucket;

import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TokenBucket {
  private final double maxTokens;
  private double currentTokens;
  private final double refillRate;
  private long lastRefilledTime;
  private final Logger logger = LoggerFactory.getLogger(TokenBucket.class);

  public TokenBucket(double maxTokens, double refillRate) {
    this.maxTokens = maxTokens;
    this.currentTokens = maxTokens;
    this.refillRate = refillRate;
    this.lastRefilledTime = new Date().getTime();
    logger.info(
        "TokenBucket Created with {} Tokens and {} refill tokens per second",
        maxTokens,
        refillRate);
  }

  public void refill() {
    long now = new Date().getTime();
    long elapsedTime = now - lastRefilledTime;

    double newTokens = elapsedTime / 1000.0 * refillRate;
    logger.info("Tokens added {}", newTokens);

    currentTokens = Math.min(maxTokens, currentTokens + newTokens);

    logger.info("Current Tokens: {}", currentTokens);
    lastRefilledTime = now;
  }

  public boolean isAllowed() {
    refill();
    if (currentTokens >= 1) {
      currentTokens -= 1;
      logger.info("Request: Allowed");
      return true;
    }
    logger.info("Request: Blocked");

    return false;
  }
}
