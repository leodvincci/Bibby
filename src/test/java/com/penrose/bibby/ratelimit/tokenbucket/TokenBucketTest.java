package com.penrose.bibby.ratelimit.tokenbucket;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class TokenBucketTest {

  @Test
  void newBucket_allowsRequestsUpToMaxTokens() {
    TokenBucket bucket = new TokenBucket(3, 1.0);

    assertTrue(bucket.isAllowed());
    assertTrue(bucket.isAllowed());
    assertTrue(bucket.isAllowed());
  }

  @Test
  void bucket_blocksWhenTokensExhausted() {
    TokenBucket bucket = new TokenBucket(2, 0.0);

    assertTrue(bucket.isAllowed());
    assertTrue(bucket.isAllowed());
    assertFalse(bucket.isAllowed());
  }

  @Test
  void bucket_refillsTokensOverTime() throws InterruptedException {
    TokenBucket bucket = new TokenBucket(1, 10.0); // 10 tokens/sec

    assertTrue(bucket.isAllowed());
    assertFalse(bucket.isAllowed());

    Thread.sleep(200); // wait for ~2 tokens to refill

    assertTrue(bucket.isAllowed());
  }

  @Test
  void bucket_doesNotExceedMaxTokensAfterRefill() throws InterruptedException {
    TokenBucket bucket = new TokenBucket(2, 100.0); // very fast refill

    Thread.sleep(200); // would refill way beyond max

    assertTrue(bucket.isAllowed());
    assertTrue(bucket.isAllowed());
    assertFalse(bucket.isAllowed()); // max is 2, should not exceed
  }

  @Test
  void bucket_withZeroRefillRate_neverRefills() {
    TokenBucket bucket = new TokenBucket(1, 0.0);

    assertTrue(bucket.isAllowed());
    assertFalse(bucket.isAllowed());
    assertFalse(bucket.isAllowed());
  }
}
