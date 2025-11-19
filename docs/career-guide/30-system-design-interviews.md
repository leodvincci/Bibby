# Section 30: System Design Interviews

**Part V: Interview Mastery — Week 30**

**Overview:**
You've prepared for technical coding interviews (Section 29). Now it's time to tackle system design interviews - where you'll design scalable backend systems and demonstrate your architectural thinking. This is where your 9 years of operational experience managing SCADA systems becomes a **significant advantage**.

**What Is a System Design Interview?**
A system design interview is a 45-60 minute session where you design a large-scale system from scratch. Instead of writing code, you'll draw diagrams, discuss trade-offs, and explain how components interact. You might design Twitter, URL shortener, rate limiter, or notification system.

**Why This Matters:**
- **Required for mid-level and senior backend roles:** Most companies with backend focus include system design
- **Tests real-world skills:** Unlike LeetCode, system design reflects actual engineering work
- **Your strongest interview:** Your operational experience with distributed systems, reliability, and scale is directly applicable
- **No "right answer":** Demonstrates how you think, communicate, and make trade-offs

**Week 30 Goals:**
1. Understand system design interview format and expectations
2. Master key system design concepts (scalability, databases, caching, load balancing)
3. Learn and practice system design framework (RADIO)
4. Study 5-7 common system design problems
5. Complete 2-3 mock system design interviews
6. Build confidence in discussing large-scale systems

**Time Commitment:** 15-20 hours (Week 30) + ongoing 5-10 hours/week until interviews

---

## Part 1: Understanding System Design Interviews

### What to Expect

**Format:**
- **Duration:** 45-60 minutes (sometimes longer for senior roles)
- **Medium:** Whiteboard (in-person) or shared drawing tool like Excalidraw (virtual)
- **Problem:** "Design [large-scale system]" with vague requirements
- **Your job:** Ask clarifying questions, define requirements, design architecture, discuss trade-offs
- **Interviewer role:** Acts as product manager/stakeholder, provides constraints when asked

**What They're Evaluating:**

**1. Problem Exploration (25%)**
- Do you ask the right clarifying questions?
- Do you identify constraints and requirements?
- Do you understand the scale?

**2. System Architecture (30%)**
- Can you design appropriate components?
- Do you understand how components interact?
- Is your design feasible and scalable?

**3. Technical Depth (20%)**
- Do you know relevant technologies (databases, caches, message queues)?
- Can you discuss trade-offs?
- Do you understand distributed systems concepts?

**4. Communication (15%)**
- Can you explain your thinking clearly?
- Do you use diagrams effectively?
- Can you justify decisions?

**5. Trade-off Analysis (10%)**
- Do you consider multiple approaches?
- Can you articulate pros/cons?
- Do you make reasonable choices given constraints?

**What They're NOT Evaluating:**
- Perfect solution (there isn't one)
- Memorization of specific system architectures
- Deep knowledge of every technology
- Implementation details (usually no code)

### Company-Specific Expectations

**For Leo's Target Companies:**

**OSIsoft, Rockwell Automation, Honeywell (Industrial Software):**
- **Focus:** Reliability, real-time data processing, time-series databases
- **Likely questions:** Design monitoring system, design alert/notification system, design data collection pipeline
- **What they value:** Operational thinking, reliability considerations, handling failures
- **Your advantage:** You've operated their products (PI System, PLCs, SCADA)

**Uptake (Startup in Industrial Space):**
- **Focus:** Data pipelines, predictive analytics, API design
- **Likely questions:** Design predictive maintenance system, design sensor data aggregation, design time-series analytics
- **What they value:** Scalability, modern architecture, cloud-native thinking
- **Your advantage:** Understanding of industrial equipment and sensor data

**Splunk, Datadog, PagerDuty (Observability/Monitoring):**
- **Focus:** Log aggregation, metrics collection, alerting systems
- **Likely questions:** Design log aggregation system, design metrics dashboard, design alerting system
- **What they value:** High throughput, real-time processing, query performance
- **Your advantage:** You've been the customer (monitoring pipelines)

**General Backend Engineering:**
- **Common questions:** URL shortener, rate limiter, notification system, newsfeed, chat system
- **Difficulty:** Medium complexity for mid-level, high complexity for senior
- **Time allocation:** 10 min requirements, 30 min design, 10 min deep dive, 10 min discussion

---

## Part 2: System Design Building Blocks

### Core Concepts You Must Know

**1. Scalability**

**Vertical Scaling (Scale Up):**
- Add more resources to single server (more CPU, RAM, disk)
- **Pros:** Simple, no code changes, maintains data consistency
- **Cons:** Hardware limits, single point of failure, expensive at scale
- **When to use:** Early stages, simpler workloads, when consistency critical

**Horizontal Scaling (Scale Out):**
- Add more servers to distribute load
- **Pros:** Unlimited scaling, fault tolerance, cost-effective
- **Cons:** Complex (load balancing, data distribution), eventual consistency
- **When to use:** High scale, need fault tolerance, cost optimization

**Example from Leo's Experience:**
```
"At Kinder Morgan, we had to scale our SCADA monitoring system as we added more pipeline
segments. We couldn't just keep adding memory to one server (vertical scaling limit), so we
distributed monitoring across regional servers (horizontal scaling). Each regional server
monitored its segments and aggregated data to central server."
```

**2. Load Balancing**

**Purpose:** Distribute incoming requests across multiple servers

**Types:**
- **Layer 4 (Transport):** Routes based on IP/port, very fast
- **Layer 7 (Application):** Routes based on content (URL, headers), more flexible

**Algorithms:**
- **Round Robin:** Distribute requests evenly (simple, works well for uniform requests)
- **Least Connections:** Send to server with fewest active connections (good for varied request lengths)
- **Weighted:** Route more traffic to more powerful servers
- **Consistent Hashing:** Route based on request attribute (e.g., user ID) for cache affinity

**Common Technologies:**
- **Hardware:** F5, Cisco
- **Software:** NGINX, HAProxy, AWS ELB/ALB

**3. Caching**

**Purpose:** Store frequently accessed data in fast storage to reduce latency

**Cache Levels:**
- **Client-side:** Browser cache, mobile app cache
- **CDN:** Static assets (images, CSS, JS) geographically distributed
- **Application:** Redis, Memcached (data cache)
- **Database:** Query result cache, index cache

**Cache Strategies:**

**Cache-Aside (Lazy Loading):**
```
1. Check cache
2. If miss, query database
3. Store result in cache
4. Return result
```
- **Pros:** Only cache what's needed, cache failure doesn't break system
- **Cons:** Cache misses are slow (3 round trips)

**Write-Through:**
```
1. Write to cache
2. Cache writes to database
3. Return success
```
- **Pros:** Cache always consistent with DB
- **Cons:** Write latency (wait for DB write)

**Write-Behind (Write-Back):**
```
1. Write to cache
2. Return success immediately
3. Cache asynchronously writes to database
```
- **Pros:** Fast writes
- **Cons:** Risk of data loss if cache fails before DB write

**Cache Invalidation:**
- **TTL (Time To Live):** Expire after fixed time (simple, works for most cases)
- **Event-driven:** Invalidate when data changes (precise, but complex)
- **LRU (Least Recently Used):** Evict oldest unused items when cache full

**4. Databases**

**SQL (Relational):**
- **Examples:** PostgreSQL, MySQL
- **When to use:**
  - Complex queries and joins
  - ACID transactions required
  - Data has clear relationships
- **Scaling:** Vertical scaling, read replicas, sharding (complex)

**NoSQL:**

**Document Store (MongoDB, Couchbase):**
- **Use case:** Flexible schema, hierarchical data
- **Example:** User profiles, product catalogs

**Key-Value (Redis, DynamoDB):**
- **Use case:** Simple lookups, caching, session storage
- **Example:** Session data, feature flags

**Column-Family (Cassandra, HBase):**
- **Use case:** Time-series data, write-heavy workloads
- **Example:** Sensor readings, logs, metrics

**Graph (Neo4j):**
- **Use case:** Relationship-heavy data
- **Example:** Social networks, recommendation engines

**Time-Series (InfluxDB, TimescaleDB):**
- **Use case:** Metrics, monitoring, sensor data
- **Example:** SCADA data, application metrics
- **Leo's advantage:** You've worked with time-series data extensively

**Database Scaling Patterns:**

**Replication:**
- **Primary-Replica:** Write to primary, read from replicas
- **Use case:** Read-heavy workloads
- **Challenge:** Replication lag (eventual consistency)

**Sharding (Horizontal Partitioning):**
- **Partition data across multiple databases**
- **Strategies:** Hash-based, range-based, geo-based
- **Use case:** Data too large for single database
- **Challenge:** Cross-shard queries, rebalancing

**5. Message Queues / Pub-Sub**

**Purpose:** Asynchronous communication between services

**Message Queue (Point-to-Point):**
- **Examples:** RabbitMQ, AWS SQS
- **Pattern:** One producer → Queue → One consumer
- **Use case:** Task processing, job queues

**Pub-Sub (Publish-Subscribe):**
- **Examples:** Kafka, AWS SNS, Redis Pub/Sub
- **Pattern:** Multiple producers → Topic → Multiple subscribers
- **Use case:** Event streaming, real-time notifications

**Why Use Message Queues:**
- **Decouple services:** Producer doesn't wait for consumer
- **Handle spikes:** Queue buffers during traffic bursts
- **Retry logic:** Failed messages can be reprocessed
- **Scale independently:** Add more consumers without changing producers

**Example:**
```
User uploads image → API puts message in queue → Background worker processes image
→ Worker sends completion notification

Without queue: User waits for image processing (30 seconds)
With queue: User gets immediate response (200ms), processing happens async
```

**6. Content Delivery Network (CDN)**

**Purpose:** Serve static content from servers geographically close to users

**How it works:**
1. User requests image from CDN
2. If CDN has cached copy, return immediately (cache hit)
3. If not, CDN fetches from origin server, caches it, returns to user
4. Subsequent requests served from CDN cache

**Use cases:**
- Static assets (images, videos, CSS, JS)
- Reduce latency for global users
- Offload traffic from origin servers

**Common providers:** CloudFront (AWS), Cloudflare, Akamai

**7. Rate Limiting**

**Purpose:** Limit number of requests from user/IP to prevent abuse

**Algorithms:**

**Token Bucket:**
- Bucket holds tokens, refills at fixed rate
- Each request consumes token
- If no tokens, request rejected
- **Pros:** Allows bursts, smooth refill
- **Cons:** More complex to implement

**Leaky Bucket:**
- Requests enter bucket, processed at fixed rate
- If bucket full, requests rejected
- **Pros:** Smooth traffic rate
- **Cons:** No burst allowance

**Fixed Window:**
- Count requests in fixed time windows (e.g., per minute)
- **Pros:** Simple to implement
- **Cons:** Burst at window boundaries

**Sliding Window:**
- Count requests in rolling time window
- **Pros:** More accurate, prevents boundary bursts
- **Cons:** More complex (store timestamps)

**8. API Design**

**REST:**
- **Resources:** /users, /posts, /comments
- **HTTP Methods:** GET (read), POST (create), PUT/PATCH (update), DELETE
- **Stateless:** Each request contains all information needed
- **Pros:** Simple, widely understood, cacheable
- **Cons:** Over-fetching/under-fetching, multiple requests for related data

**GraphQL:**
- **Query language:** Client specifies exactly what data needed
- **Single endpoint:** /graphql
- **Pros:** No over/under-fetching, single request for complex data
- **Cons:** Caching harder, query complexity, learning curve

**WebSockets:**
- **Bi-directional:** Server can push to client
- **Persistent connection**
- **Use case:** Real-time updates (chat, notifications, live dashboards)

**9. Microservices vs Monolith**

**Monolith:**
- Single application with all features
- **Pros:** Simple to develop/deploy, easier consistency, lower latency (in-process calls)
- **Cons:** Hard to scale, hard to maintain (large codebase), all-or-nothing deployment

**Microservices:**
- Multiple small services, each owns specific domain
- **Pros:** Independent scaling, independent deployment, team autonomy
- **Cons:** Complex (distributed system), network latency, data consistency challenges

**When to use each:**
- **Start with monolith** if team is small, domain is unclear
- **Move to microservices** when need independent scaling, have clear domain boundaries, have team capacity for complexity

---

## Part 3: The System Design Framework (RADIO)

### Step-by-Step Approach for Every Problem

**R - Requirements**
**A - Architecture**
**D - Data Model**
**I - Interface**
**O - Optimization**

### Step 1: Requirements (8-12 minutes)

**Goal:** Understand what you're building and the constraints.

**Functional Requirements (What should the system do?):**

Ask questions like:
- What are the core features?
- What's in scope vs out of scope?
- What are the user flows?

**Example - Design URL Shortener:**
```
You: "Let me clarify the functional requirements. Should the system:
1. Generate short URLs from long URLs?
2. Redirect short URLs to long URLs?
3. Allow custom short URLs?
4. Track analytics (click counts)?
5. Allow users to delete or expire URLs?

For this interview, should I focus on just generation and redirection, or include
analytics and custom URLs?"

Interviewer: "Focus on generation and redirection. Analytics is optional if you have time."
```

**Non-Functional Requirements (How should it perform?):**

Ask about:
- **Scale:** How many users? How many requests per second?
- **Performance:** What's acceptable latency?
- **Availability:** How critical is uptime? (99.9% vs 99.99%)
- **Consistency:** Strong consistency or eventual consistency okay?

**Example - URL Shortener:**
```
You: "Let me understand the scale:
1. How many URLs will be created per day?
2. What's the read-to-write ratio? (More redirects than creations?)
3. What's the expected lifespan of URLs?
4. What's acceptable latency for redirect? For generation?
5. If a URL is created, how quickly must it be available for redirect?"

Interviewer: "100 million URLs created per day, 100:1 read-to-write ratio. URLs live
forever. Redirect latency should be under 50ms. Generation can be a few hundred ms.
Redirect should work immediately after creation."

You: "Great. Let me calculate rough numbers:
- 100M URLs/day ≈ 1,200 URLs/second
- 100:1 ratio = 120,000 reads/second
- Storage: If each URL is 500 bytes, 100M/day * 365 days * 10 years = 182TB
- This is a read-heavy, high-scale system requiring low latency reads."
```

**Write Down Requirements:**
```
Functional:
- Generate short URL from long URL
- Redirect short URL to long URL
- URLs never expire

Non-Functional:
- 1,200 writes/second
- 120,000 reads/second
- < 50ms redirect latency
- Strong consistency (redirect works immediately)
- High availability (URL shortener is critical service)
```

### Step 2: Architecture (20-25 minutes)

**Goal:** Design high-level architecture and major components.

**2.1 High-Level Design (5-7 minutes)**

Draw boxes for major components:

**Example - URL Shortener:**
```
┌─────────┐
│  Client │
└────┬────┘
     │
     ▼
┌────────────────┐
│  Load Balancer │
└────┬───────────┘
     │
     ▼
┌────────────────┐
│  API Servers   │  (Stateless, horizontally scalable)
└────┬───────────┘
     │
     ├─────────────────┐
     ▼                 ▼
┌────────────┐    ┌──────────┐
│  Database  │    │  Cache   │  (Redis - for fast reads)
│ (Primary + │    │ (Redis)  │
│  Replicas) │    └──────────┘
└────────────┘
```

Explain:
```
"I'll use a simple architecture:
1. Load balancer distributes traffic across API servers
2. API servers handle generation and redirect logic
3. Database stores mappings (short URL -> long URL)
4. Cache (Redis) for fast redirects (reduce DB load)
5. Database replicas for read scalability"
```

**2.2 API Design (3-5 minutes)**

Define key APIs:

**Example - URL Shortener:**
```
POST /api/v1/shorten
Request:  { "long_url": "https://example.com/very/long/url" }
Response: { "short_url": "https://short.ly/abc123" }

GET /abc123
Response: 302 Redirect to long URL
```

**2.3 Core Components (10-12 minutes)**

Dive into each component:

**URL Generation Service:**
```
You: "For generating short URLs, I need a unique short code. A few approaches:

1. Hash-based: Hash long URL, take first 6-8 characters
   - Pros: Deterministic (same long URL = same short URL)
   - Cons: Collision possible, need to handle duplicates

2. Counter-based: Global counter, convert to base62
   - Pros: No collisions, predictable
   - Cons: Need distributed counter, sequential (security concern)

3. Random + collision check: Generate random string, check if exists
   - Pros: Simple
   - Cons: Collision rate increases over time

I'll use base62 encoding of a distributed counter:
- Counter generates unique ID (e.g., 12345)
- Convert to base62: 12345 → 'dnh' (using [a-zA-Z0-9])
- 6 characters base62 = 62^6 = 56 billion possible URLs
- This gives us years of capacity

For distributed counter, I'll use a service that generates unique IDs across servers,
like Twitter Snowflake or database auto-increment with ranges."
```

**Redirect Service:**
```
You: "For redirects:

1. Client requests GET /abc123
2. API server checks Redis cache for 'abc123'
3. If cache hit: Return long URL immediately (< 5ms)
4. If cache miss:
   - Query database for 'abc123' -> long URL
   - Store in Redis cache (TTL = 1 day)
   - Return long URL (< 50ms)
5. API returns 302 redirect to long URL

Cache hit rate should be very high (99%+) given 100:1 read:write ratio and
hot URLs being accessed repeatedly."
```

**2.4 Data Flow (2-3 minutes)**

Walk through user flow:

**Create Short URL:**
```
1. User sends POST /api/v1/shorten with long URL
2. API server requests unique ID from ID generation service
3. Convert ID to base62 string (short code)
4. Store mapping in database: (short_code, long_url, created_at)
5. Store in cache for immediate availability
6. Return short URL to user
```

**Redirect:**
```
1. User visits short.ly/abc123
2. Load balancer routes to API server
3. API checks Redis: GET abc123
4. Cache hit (99% case): Return long URL in 5ms
5. Cache miss: Query DB, populate cache, return long URL in 50ms
6. API returns 302 redirect
7. Browser redirects to long URL
```

### Step 3: Data Model (5-7 minutes)

**Goal:** Define database schema and data storage.

**Example - URL Shortener:**

**Main Table - URL Mappings:**
```sql
CREATE TABLE url_mappings (
    id BIGINT PRIMARY KEY,              -- Same as the generated ID
    short_code VARCHAR(10) UNIQUE,      -- 'abc123'
    long_url TEXT NOT NULL,             -- Original URL
    created_at TIMESTAMP DEFAULT NOW(),
    INDEX idx_short_code (short_code)   -- Fast lookup by short code
);
```

**Database Choice:**
```
You: "I'll use PostgreSQL for the primary database:

Pros:
- ACID transactions (strong consistency requirement)
- Reliable
- Good read performance with indexes and replicas
- Mature ecosystem

For caching, Redis:
- In-memory, very fast (sub-millisecond)
- TTL support
- Simple key-value for this use case

Schema is simple: We only need one table. Short code is unique index for fast lookups."
```

**Data Storage Calculation:**
```
- 100M URLs/day
- Each record ≈ 500 bytes (ID + short_code + long_url + timestamp)
- Daily storage: 100M * 500 bytes = 50 GB/day
- Yearly: 50 GB * 365 = 18.25 TB/year
- 5 years: ~90 TB

With database compression and actual URL lengths, probably 50-70 TB for 5 years.
This fits in modern databases with sharding if needed.
```

### Step 4: Interface (Optional - 3-5 minutes)

**Goal:** Discuss external interfaces, APIs, or protocols.

**For URL Shortener:**
- REST API (already discussed in Architecture)
- HTTP 302 redirect for short URL access
- Could add: Analytics API, custom domain support, QR code generation

**Often skip this step if time is tight** - it's covered in Architecture.

### Step 5: Optimization & Deep Dive (10-12 minutes)

**Goal:** Identify bottlenecks and optimize. Go deep on interesting areas.

**5.1 Bottleneck Analysis:**

```
You: "Let me identify potential bottlenecks:

1. Database writes (1,200/second)
   - Modern PostgreSQL can handle 10,000+ writes/second on good hardware
   - Not a bottleneck initially
   - If it becomes one: Shard database by short_code hash

2. Database reads (120,000/second)
   - This is the bottleneck
   - Solution: Redis cache + read replicas
   - With 99% cache hit rate: Only 1,200 DB reads/second
   - Spread across 3-5 read replicas = 240-400 reads/replica/second
   - Very manageable

3. Cache capacity
   - If we cache all URLs: 182 TB (not feasible in memory)
   - Cache hot URLs only: 80/20 rule - 20% of URLs = 80% of traffic
   - 20% of 100M URLs = 20M URLs = 10 GB (fits in Redis easily)
   - With LRU eviction, cache stays lean

4. ID generation service
   - 1,200 IDs/second
   - Can pre-generate ID ranges and distribute to API servers
   - Each server gets range (e.g., Server 1: 1-100,000, Server 2: 100,001-200,000)
   - No coordination needed for most requests
```

**5.2 Deep Dive Topics (Choose 1-2 Based on Time):**

**Interviewer might ask:** "How would you handle 10x traffic growth?"

```
You: "For 10x growth (1.2M reads/second):

1. Database reads still not an issue with high cache hit rate
2. Cache layer needs scaling:
   - Shard Redis: Hash short_code to multiple Redis instances
   - Consistent hashing to minimize resharding impact
   - Each Redis instance handles subset of URLs

3. API servers: Horizontally scale (stateless, easy)

4. Database writes (12,000/second):
   - May need to shard database by short_code range or hash
   - Use database proxy (ProxySQL, Vitess) to route queries to correct shard

5. ID generation:
   - Snowflake-style ID generation (timestamp + server ID + sequence)
   - Each server generates IDs independently
   - No coordination needed
```

**Interviewer might ask:** "How would you ensure high availability?"

```
You: "For high availability:

1. Multiple availability zones:
   - Deploy API servers across 3 AZs
   - Database primary in AZ-1, replicas in AZ-2 and AZ-3
   - If AZ-1 fails, promote replica to primary

2. Health checks and auto-recovery:
   - Load balancer health checks API servers
   - Remove unhealthy servers from rotation
   - Auto-scaling group replaces failed instances

3. Database failover:
   - Automated primary failover (AWS RDS, PostgreSQL streaming replication)
   - Typically 30-60 seconds downtime
   - Read replicas continue serving during failover

4. Cache failure:
   - If Redis fails, fall back to database
   - Performance degrades but system stays up
   - Cache is acceleration, not critical path

5. Monitoring and alerts:
   - Monitor API latency, error rates, database connections
   - Alert on-call engineer if thresholds exceeded

With this design, we can achieve 99.9%+ availability."
```

**Interviewer might ask:** "What about security?"

```
You: "Security considerations:

1. Malicious URLs:
   - URL validation: Check against blocklist of known malicious domains
   - Optional: Integrate with Google Safe Browsing API
   - Rate limiting: Prevent abuse (e.g., 100 URLs/hour per user)

2. Spam/abuse:
   - CAPTCHA for URL creation (prevent bots)
   - Rate limiting by IP and user account
   - Monitor for patterns (same long_url created many times)

3. Sequential ID exposure:
   - Base62 encoding makes IDs non-obvious
   - Could add random offset or encryption
   - Or use random IDs instead of sequential

4. DDoS protection:
   - CDN/WAF (Cloudflare) in front of load balancer
   - Rate limiting at multiple layers

5. Data privacy:
   - HTTPS only
   - Don't log or store user data unnecessarily
```

---

## Part 4: Common System Design Questions

### Question 1: Design a URL Shortener

**Covered above in framework example.**

**Key points:**
- ID generation (base62, Snowflake)
- Caching for read-heavy workload
- Database choice and schema
- Scalability (sharding, replication)

**Leo's angle:**
"Similar to assigning unique identifiers to pipeline segments and sensors in SCADA - need globally unique IDs and fast lookup."

---

### Question 2: Design a Rate Limiter

**Requirements:**
- Limit API requests per user (e.g., 100 requests/minute)
- Distributed system (multiple API servers)
- Low latency (< 10ms overhead)

**Architecture:**

```
┌─────────┐
│  Client │
└────┬────┘
     │
     ▼
┌────────────────┐
│  API Gateway   │ ← Rate limiting logic here
└────┬───────────┘
     │
     ▼
┌────────────────┐     ┌──────────────┐
│  API Servers   │────▶│  Redis       │ (Store request counts)
└────────────────┘     │  (Counter)   │
                       └──────────────┘
```

**Algorithm Choice: Sliding Window Log**

```
Redis data structure:
Key: "rate_limit:user_123"
Value: Sorted Set of timestamps

ZADD rate_limit:user_123 1678901234 1678901234  (Add request timestamp)
ZREMRANGEBYSCORE rate_limit:user_123 0 (now - 60s)  (Remove old entries)
ZCARD rate_limit:user_123  (Count entries)

If count > 100: Reject request
Else: Allow request
```

**Optimizations:**
- Use fixed window counter for simplicity (trade accuracy for performance)
- Use local cache + Redis (reduce Redis load)
- Pre-compute limits in memory, sync to Redis periodically

**Leo's angle:**
"Rate limiting is like flow control in pipeline SCADA - can't exceed pressure thresholds or you risk failures. Need real-time monitoring and immediate response."

---

### Question 3: Design a Notification System

**Requirements:**
- Send notifications via email, SMS, push notification
- Support millions of users
- Handle notification preferences
- Deliver notifications reliably

**Architecture:**

```
┌─────────────────┐
│  Notification   │
│    Trigger      │ (User action, scheduled event, system alert)
└────┬────────────┘
     │
     ▼
┌─────────────────┐
│  Notification   │
│    Service      │ (Determines who to notify, what channel)
└────┬────────────┘
     │
     ▼
┌─────────────────┐
│  Message Queue  │ (Kafka, SQS - decouple sender from delivery)
└────┬────────────┘
     │
     ├──────────┬──────────────┐
     ▼          ▼              ▼
┌─────────┐ ┌─────────┐  ┌──────────┐
│  Email  │ │   SMS   │  │   Push   │ (Worker pools for each channel)
│ Worker  │ │ Worker  │  │  Worker  │
└─────────┘ └─────────┘  └──────────┘
     │          │              │
     ▼          ▼              ▼
┌─────────┐ ┌─────────┐  ┌──────────┐
│SendGrid │ │ Twilio  │  │   FCM    │ (Third-party services)
└─────────┘ └─────────┘  └──────────┘
```

**Data Model:**

```sql
-- User notification preferences
CREATE TABLE user_preferences (
    user_id BIGINT PRIMARY KEY,
    email_enabled BOOLEAN DEFAULT TRUE,
    sms_enabled BOOLEAN DEFAULT FALSE,
    push_enabled BOOLEAN DEFAULT TRUE
);

-- Notification events
CREATE TABLE notifications (
    id BIGINT PRIMARY KEY,
    user_id BIGINT,
    type VARCHAR(50),  -- 'order_shipped', 'price_alert', etc.
    channel VARCHAR(20),  -- 'email', 'sms', 'push'
    status VARCHAR(20),  -- 'pending', 'sent', 'failed'
    created_at TIMESTAMP,
    sent_at TIMESTAMP,
    INDEX idx_user_created (user_id, created_at)
);
```

**Workflow:**

1. **Trigger:** Order shipped → Notification Service
2. **Check preferences:** Query user_preferences for user
3. **Create notification:** Insert into notifications table (status = 'pending')
4. **Enqueue message:** Push to appropriate queue (email_queue, sms_queue, push_queue)
5. **Workers consume:** Email worker picks message from email_queue
6. **Send:** Call SendGrid API to send email
7. **Update status:** Update notification (status = 'sent', sent_at = now)
8. **Retry on failure:** If send fails, retry with exponential backoff (max 3 retries)

**Optimizations:**
- **Batching:** Group emails to same user (e.g., daily digest instead of 50 separate emails)
- **Prioritization:** Urgent notifications (security alerts) in separate high-priority queue
- **Rate limiting:** Don't spam users (max 10 emails/day)
- **Template caching:** Cache email templates in Redis
- **Analytics:** Track delivery rates, open rates, click rates

**Leo's angle:**
"This is similar to SCADA alerting - when pressure threshold exceeded, notify operators via multiple channels. Need reliability (can't miss critical alerts), priority handling (leak alert > routine maintenance), and avoiding alert fatigue."

---

### Question 4: Design Twitter News Feed

**Requirements:**
- User follows other users
- Show recent tweets from people you follow
- Fast feed generation (< 200ms)
- Support millions of users

**Architecture:**

**Approach 1: Fan-out on Write (Push Model)**

```
When user posts tweet:
1. Get list of all followers (1M followers)
2. For each follower, insert tweet into their feed cache
3. When follower loads feed, read from their pre-computed feed cache

Pros: Fast reads (feed pre-computed)
Cons: Slow writes for celebrities (1M cache updates), wasted work (inactive users)
```

**Approach 2: Fan-out on Read (Pull Model)**

```
When user loads feed:
1. Get list of people they follow (1K following)
2. For each, get their recent tweets (1K queries)
3. Merge and sort tweets
4. Return top 20

Pros: No wasted work
Cons: Slow reads (1K queries), expensive merge/sort
```

**Hybrid Approach (Best):**

```
- Regular users: Fan-out on write (< 1M followers)
- Celebrities: Fan-out on read (> 1M followers)

When user loads feed:
1. Retrieve pre-computed feed from cache (from non-celebrities)
2. Query celebrity tweets directly (if user follows any)
3. Merge and sort
4. Return top 20

This balances read/write costs.
```

**Data Model:**

```sql
-- Tweets
CREATE TABLE tweets (
    id BIGINT PRIMARY KEY,
    user_id BIGINT,
    content TEXT,
    created_at TIMESTAMP,
    INDEX idx_user_created (user_id, created_at)
);

-- Follower relationships
CREATE TABLE follows (
    follower_id BIGINT,
    followee_id BIGINT,
    PRIMARY KEY (follower_id, followee_id),
    INDEX idx_followee (followee_id)  -- Get all followers of a user
);

-- Pre-computed feeds (cache)
CREATE TABLE feeds (
    user_id BIGINT,
    tweet_id BIGINT,
    created_at TIMESTAMP,
    PRIMARY KEY (user_id, tweet_id),
    INDEX idx_user_created (user_id, created_at DESC)
);
```

**Feed Generation Service:**
```
1. User posts tweet
2. Determine fan-out strategy:
   - If user has < 1M followers: Push to all follower feeds
   - If user has > 1M followers: Don't push (pull on read)
3. For push model:
   - Get follower IDs from follows table
   - Insert into feeds table for each follower
   - Update Redis cache for active users
```

**Feed Retrieval:**
```
1. User requests feed
2. Check Redis cache: feed:user_123
3. If cache hit: Return feed
4. If cache miss:
   - Query feeds table: SELECT tweet_id FROM feeds WHERE user_id = 123 ORDER BY created_at DESC LIMIT 20
   - For each tweet_id, get tweet details
   - Store in Redis cache (TTL = 5 min)
   - Return feed
```

**Scalability:**
- **Sharding:** Partition tweets and feeds by user_id hash
- **Caching:** Heavy caching for hot users and recent tweets
- **CDN:** Serve static assets (images, videos) from CDN
- **Read replicas:** Replicate database for read scalability

---

### Question 5: Design a Chat System (Messaging App)

**Requirements:**
- 1-on-1 messaging
- Group chat
- Real-time delivery
- Message history
- Read receipts

**Architecture:**

```
┌─────────┐                         ┌─────────┐
│ Client  │◀───── WebSocket ──────▶│ Client  │
└────┬────┘                         └────┬────┘
     │                                    │
     ▼                                    ▼
┌────────────────────────────────────────────┐
│         WebSocket Servers (Stateful)       │
└──────────────┬─────────────────────────────┘
               │
               ▼
┌──────────────────────────┐    ┌─────────────┐
│    Message Queue         │───▶│   Database  │
│    (Kafka)               │    │ (Messages)  │
└──────────────────────────┘    └─────────────┘
```

**Why WebSocket?**
- Persistent connection for real-time bi-directional communication
- Server can push messages to client immediately
- Low latency (no polling)

**Message Flow:**

**1-on-1 Chat:**
```
1. User A sends message to User B
2. Client connects to WebSocket server via persistent connection
3. WebSocket server:
   - Stores message in database
   - Publishes to Kafka topic: "user_B_messages"
4. User B's WebSocket server (subscribed to "user_B_messages") receives message
5. Push message to User B's client over WebSocket
6. User B's client sends read receipt
7. Receipt flows back to User A via same path
```

**Group Chat:**
```
1. User A sends message to Group (100 members)
2. WebSocket server:
   - Store message in database
   - Publish to Kafka topic: "group_123_messages"
3. All group members' WebSocket servers (subscribed to "group_123_messages") receive message
4. Each server pushes to respective clients
```

**Challenges:**

**User Online/Offline:**
- Store connection mapping: user_id → websocket_server_id (in Redis)
- When message arrives, check Redis to find which server user is connected to
- If offline: Store message, send push notification

**Message History:**
- Store all messages in database (Cassandra for write-heavy workload)
- When user opens chat, load last 50 messages
- Pagination for older messages

**Scalability:**
- WebSocket servers are stateful (each maintains connections)
- Use consistent hashing to route users to same server (or replicate state)
- Kafka provides fan-out (1 write, N reads for group chats)

**Data Model:**

```sql
-- Messages
CREATE TABLE messages (
    id BIGINT PRIMARY KEY,
    sender_id BIGINT,
    receiver_id BIGINT,  -- NULL for group messages
    group_id BIGINT,  -- NULL for 1-on-1
    content TEXT,
    created_at TIMESTAMP,
    INDEX idx_conversation (sender_id, receiver_id, created_at),
    INDEX idx_group (group_id, created_at)
);

-- Read receipts
CREATE TABLE read_receipts (
    message_id BIGINT,
    user_id BIGINT,
    read_at TIMESTAMP,
    PRIMARY KEY (message_id, user_id)
);
```

**Leo's angle:**
"Real-time communication is like SCADA telemetry - sensor data must reach operators immediately. We used pub-sub pattern where sensors published to topics and monitoring systems subscribed. Similar architecture here with Kafka and WebSockets."

---

## Part 5: Leo's Operational Advantage

### How Your SCADA/Pipeline Experience Translates

**Your Background:**
- 9 years managing petroleum pipeline SCADA systems
- Monitored 1,000+ miles of pipeline across multiple states
- Handled real-time data from hundreds of sensors
- Managed alerts, failures, and incident response
- Used OSIsoft PI System for time-series data

**This Directly Maps to System Design:**

**1. You Understand Scale and Reliability**

**In Interview, You Can Say:**
```
"Managing pipeline SCADA, we had 500+ sensors sending data every second - that's 43 million
data points per day. We needed sub-second response for critical alerts like pressure spikes.
This experience helps me think about handling high-throughput systems and meeting strict
latency requirements."
```

**2. You Think About Failure Modes**

**In Interview:**
```
"In pipeline operations, we planned for every failure scenario - sensor failure, network
outage, power loss. For this design, I'd apply the same thinking: What if the database
goes down? What if Redis fails? What if network partitions? I'd design for graceful
degradation and fail-safes."
```

**3. You Understand Monitoring and Observability**

**In Interview:**
```
"We had dashboards showing pipeline health in real-time - flow rates, pressures, valve
states. When designing systems, I'd include comprehensive monitoring from day one: API
latency metrics, error rates, database connection pools, cache hit rates. You can't
operate what you can't see."
```

**4. You Know Time-Series Data**

**In Interview (for time-series questions):**
```
"I've worked extensively with OSIsoft PI System, which stores billions of time-series
data points. For this metrics aggregation system, I'd use a time-series database like
InfluxDB or TimescaleDB optimized for write-heavy workloads and time-range queries.
I'd implement downsampling for historical data - keep 1-second granularity for recent
data, rollup to 1-minute for last month, 1-hour for last year."
```

**5. You Understand Real-World Constraints**

**In Interview:**
```
"In SCADA, we couldn't just restart systems for updates - pipelines run 24/7. This taught
me to design for zero-downtime deployments: blue-green deployments, rolling updates,
backward-compatible API changes. I'd apply the same here."
```

### System Design Questions Where You Shine

**1. Design a Monitoring/Alerting System**
- **Direct experience:** You've been the user of these systems
- **You know:** What operators need, false positive problems, alert fatigue, escalation policies

**2. Design a Time-Series Database**
- **Direct experience:** Worked with PI System
- **You know:** Compression, retention policies, query patterns, aggregation

**3. Design a Real-Time Dashboard**
- **Direct experience:** Used SCADA HMIs
- **You know:** Data freshness requirements, handling stale data, visualization needs

**4. Design a Data Pipeline**
- **Direct experience:** Managed data flow from sensors to storage to analytics
- **You know:** ETL, data validation, handling late/missing data

**5. Design a Distributed System with High Availability**
- **Direct experience:** Pipeline operations require 99.9%+ uptime
- **You know:** Redundancy, failover, disaster recovery

### Use Your Experience Naturally

**DON'T:**
- Force SCADA references into every answer
- Spend too much time on analogies

**DO:**
- Mention operational experience when relevant
- Use concrete examples from your background
- Show you've operated large-scale systems
- Demonstrate you think about reliability and failure modes

**Example:**
```
Interviewer: "How would you handle a database outage?"

You: "Great question. In pipeline operations, we had redundant systems for critical
components - if the primary SCADA server failed, a hot standby took over automatically.
I'd apply similar thinking here:

1. Database primary-replica setup with automated failover
2. Health checks every 10 seconds
3. If primary fails, promote replica to primary (30-60 second downtime)
4. Application layer retries with exponential backoff during transition
5. Monitoring and alerting for operations team

This gives us high availability without building overly complex active-active setup."
```

This shows:
- Real-world experience with failure handling
- Concrete solution
- Understanding of trade-offs
- Operations-minded thinking

---

## Part 6: Week 30 Action Plan

### Day-by-Day Practice Schedule

**Day 1 (Monday): 3-4 hours**
- [ ] Read: System design primer (GitHub - donnemartin/system-design-primer) - 90 min
- [ ] Watch: "System Design Interview" videos (Gaurav Sen) - 60 min
- [ ] Study: RADIO framework - 30 min
- [ ] Practice: Design URL Shortener (solo) - 60 min

**Day 2 (Tuesday): 3-4 hours**
- [ ] Study: Database concepts (SQL vs NoSQL, replication, sharding) - 60 min
- [ ] Study: Caching strategies - 30 min
- [ ] Practice: Design Rate Limiter (solo) - 90 min
- [ ] Review: Compare your design to reference solution - 30 min

**Day 3 (Wednesday): 3-4 hours**
- [ ] Study: Message queues, pub-sub patterns - 60 min
- [ ] Study: Load balancing, CDN - 30 min
- [ ] Practice: Design Notification System (solo) - 90 min
- [ ] Review: Identify gaps in your knowledge - 30 min

**Day 4 (Thursday): 3-4 hours**
- [ ] Study: Microservices, API design - 60 min
- [ ] Practice: Design Twitter News Feed (solo) - 120 min
- [ ] Review: Alternative approaches - 30 min

**Day 5 (Friday): 3-4 hours**
- [ ] Study: WebSockets, real-time systems - 45 min
- [ ] Practice: Design Chat System (solo) - 120 min
- [ ] Create cheat sheet of common patterns - 45 min

**Day 6 (Saturday): 4-5 hours**
- [ ] Review: All 5 designs from week - 60 min
- [ ] **Mock interview #1:** URL Shortener with friend/Pramp - 60 min
- [ ] Debrief and note improvements - 30 min
- [ ] **Mock interview #2:** Design question of your choice - 60 min
- [ ] Study: Fill knowledge gaps identified in mocks - 60 min

**Day 7 (Sunday): 3-4 hours**
- [ ] Practice: Design a system relevant to target company:
  - For OSIsoft: Design time-series data collection system - 90 min
  - For Uptake: Design predictive maintenance data pipeline - 90 min
  - For Datadog: Design metrics aggregation system - 90 min
- [ ] **Mock interview #3:** Practice with new question - 60 min
- [ ] Week review: Document learnings and patterns - 30 min
- [ ] Plan for Week 31 - 15 min

**Total: 19-26 hours**

---

## Part 7: Practice Effectively

### How to Practice Solo

**1. Set Timer (45-60 minutes)**
- Mimic real interview conditions
- Forces you to manage time

**2. Talk Out Loud**
- Explain your thinking as if interviewer is listening
- "I'm going to start by clarifying requirements..."
- "Now I'll draw the high-level architecture..."

**3. Draw Diagrams**
- Use Excalidraw, draw.io, or paper
- Practice drawing boxes and arrows clearly
- Label everything

**4. Cover All Steps**
- Don't skip requirements gathering (common mistake)
- Don't skip trade-off discussion
- Practice full RADIO framework every time

**5. Review Reference Solutions**
- After your attempt, compare to reference designs
- Note what you missed
- Understand alternative approaches

### Mock Interview Format

**Setup:**
- 60 minutes total
- Use video call + shared drawing tool (Excalidraw, Miro)
- One person is interviewer, one is candidate

**Interviewer Responsibilities:**
- Choose a problem you understand well
- Present problem with minimal details
- Act as product manager: Answer questions when asked, provide constraints
- Provide hints if candidate stuck for 10+ minutes
- Take notes on: requirements gathering, architecture, communication, trade-offs
- Give feedback at end

**Candidate Responsibilities:**
- Use RADIO framework
- Talk through your thinking
- Ask clarifying questions
- Draw clear diagrams
- Discuss trade-offs

**Post-Interview Debrief (15 minutes):**
- What went well?
- What could be improved?
- Did candidate ask good questions?
- Was architecture reasonable?
- How was communication?

---

## Part 8: Resources

### Free Resources

**Best System Design Primer:**
- **GitHub:** donnemartin/system-design-primer
- Comprehensive guide to system design concepts
- Sample interview questions with solutions

**YouTube Channels:**
- **Gaurav Sen** - Clear explanations, visual diagrams
- **Tech Dummies (Narendra L)** - Good for beginners
- **ByteByteGo** - High-quality animations

**Blogs:**
- **High Scalability** (highscalability.com) - Real system architectures
- **Engineering blogs:** Uber, Netflix, Airbnb, Meta engineering blogs

### Paid Resources (Optional)

**Courses:**
- **Grokking the System Design Interview** (educative.io) - $79
  - 16 classic system design problems with solutions
  - Good for structured learning
- **ByteByteGo** (bytebytego.com) - $15/month
  - Alex Xu (author of "System Design Interview" book)
  - Visual explanations

**Books:**
- **"System Design Interview" by Alex Xu** (Vol 1 and 2) - $30 each
  - Best system design interview books
  - Clear diagrams and explanations
- **"Designing Data-Intensive Applications" by Martin Kleppmann** - $50
  - More technical, for deeper understanding
  - Not interview-focused, but excellent for learning

### Mock Interview Platforms

- **Pramp** (pramp.com) - Free peer-to-peer
- **Interviewing.io** - Free and paid tiers
- **Hello Interview** (hellointerview.com) - System design focused

---

## Part 9: Common Mistakes and How to Avoid Them

### Mistake 1: Jumping to Solution Too Quickly

**What it looks like:**
- Immediately drawing architecture
- Not asking any questions
- Assuming requirements

**Why it's bad:**
- Might solve wrong problem
- Interviewer can't evaluate your requirements gathering
- Shows lack of real-world experience

**How to avoid:**
- Spend 8-12 minutes on requirements
- Ask about scale, latency, consistency, features
- Clarify ambiguities

### Mistake 2: Getting Lost in Details Too Early

**What it looks like:**
- Spending 20 minutes on database schema design
- Discussing code implementation
- Focusing on one component, ignoring others

**Why it's bad:**
- Run out of time
- Don't cover full system
- Miss high-level thinking

**How to avoid:**
- Start with high-level architecture (boxes and arrows)
- Go deep only when interviewer asks
- Manage time: 10 min requirements, 25 min architecture, 10 min deep dive

### Mistake 3: Not Discussing Trade-offs

**What it looks like:**
- Present one solution without alternatives
- Don't explain why you chose approach
- Ignore pros/cons

**Why it's bad:**
- System design is about trade-offs
- Shows you think there's one "right answer" (there isn't)

**How to avoid:**
- For each decision, mention alternative
- "I could use SQL or NoSQL. SQL gives us ACID transactions but harder to scale. NoSQL scales easily but eventual consistency. Given strong consistency requirement, I'll use SQL."

### Mistake 4: Ignoring Non-Functional Requirements

**What it looks like:**
- Only discuss features
- Don't ask about scale, latency, availability

**Why it's bad:**
- These drive architectural decisions
- Real systems must meet performance/reliability goals

**How to avoid:**
- Always ask: How many users? Requests per second? Latency requirements? Consistency needs?
- Calculate rough numbers (back-of-envelope)

### Mistake 5: Poor Communication

**What it looks like:**
- Long silences while thinking
- Messy diagrams with unlabeled boxes
- Mumbling through design

**Why it's bad:**
- Interviewer can't follow your thinking
- Can't help if you're stuck
- Communication is evaluated skill

**How to avoid:**
- Think out loud: "I'm considering whether to use cache here..."
- Draw clear diagrams with labels
- Explain as you draw: "This is the load balancer, which distributes traffic to..."

---

## Part 10: Integration with Other Sections

**This Section Builds On:**
- **Sections 1-16 (Foundation):** Technical knowledge (databases, Spring Boot, architecture) now applied at system level
- **Section 24 (Flagship Story):** Your SCADA experience becomes unique advantage in system design
- **Section 29 (Technical Interviews):** Often combined - technical coding + system design in same interview loop

**This Section Prepares For:**
- **Section 31 (Behavioral Interviews):** Often combined with system design ("Tell me about a time you designed a system...")
- **Section 32 (Job Search):** System design competence is key differentiator for backend roles

---

## Part 11: Final Thoughts

**System design interviews are where you shine.**

Most bootcamp grads struggle with system design because they lack real-world experience. You have:
- **9 years operating large-scale systems** (SCADA managing 1,000+ miles of pipeline)
- **Reliability mindset** (pipeline can't go down)
- **Understanding of distributed systems** (sensors, regional servers, central monitoring)
- **Real-world constraints** (latency, failures, monitoring)

**This is your advantage. Use it.**

**By the end of Week 30, you should:**
- Understand system design interview format
- Master core concepts (scalability, caching, databases, message queues)
- Be comfortable with RADIO framework
- Have practiced 5-7 common questions
- Completed 2-3 mock interviews
- Feel confident discussing large-scale systems

**Remember:**
- **No perfect solution** - it's about trade-offs and communication
- **Your operational background is differentiating** - don't hide it
- **Draw, draw, draw** - visual diagrams are critical
- **Think out loud** - interviewer can't read your mind
- **Ask questions** - ambiguity is intentional

**System design is less about memorization, more about thinking.** Your 9 years managing critical infrastructure has prepared you for this.

**Next:** Section 31 will cover behavioral interviews - where you'll tell stories from your operational background to demonstrate leadership, problem-solving, and resilience.

---

**End of Section 30**
