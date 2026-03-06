package com.penrose.bibby.ratelimit;

import com.penrose.bibby.ratelimit.tokenbucket.TokenBucket;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class RateLimitService {
  private final ConcurrentHashMap<String, TokenBucket> buckets;

  public RateLimitService() {
    this.buckets = new ConcurrentHashMap<>();
  }

  public boolean isAllowed(String ipAddress) {
    buckets.putIfAbsent(ipAddress, new TokenBucket(5, .05));
    return buckets.get(ipAddress).isAllowed();
  }
}
