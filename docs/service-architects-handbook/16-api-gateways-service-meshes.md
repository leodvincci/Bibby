# Section 16: API Gateways & Service Meshes

**Part V: Production Excellence**

---

## The Chaos of Direct Service-to-Service Communication

You've built 15 microservices. Each service needs:
- **Authentication:** Validate JWT tokens
- **Rate limiting:** Prevent abuse
- **SSL termination:** HTTPS for all services
- **CORS:** Allow browser requests
- **Request logging:** Track every API call
- **Metrics:** Instrument all endpoints
- **Circuit breaking:** Handle downstream failures

**Option 1:** Implement in every service.
- **Result:** 15 copies of the same code. Security patch? Update 15 services.

**Option 2:** Use an **API Gateway** (north-south traffic) and **Service Mesh** (east-west traffic).
- **Result:** Centralized cross-cutting concerns. One place to update.

In this section, I'll show you when to use each, how to deploy them with Bibby, and the trade-offs involved.

---

## What Is an API Gateway?

**Definition:** Single entry point for all client requests. Routes to appropriate backend services.

**Mental model:** The receptionist at a building. All visitors go through reception; receptionist directs them to the right office.

### Traffic Pattern: North-South

```
        Internet (Client)
              │
              ▼
        ┌──────────┐
        │   API    │  ← Single public endpoint
        │  Gateway │
        └──────────┘
         │    │    │
    ┌────┘    │    └────┐
    ▼         ▼         ▼
┌────────┐ ┌────────┐ ┌────────┐
│Catalog │ │Library │ │ Circ.  │  ← Internal services
│Service │ │Service │ │Service │     (not publicly exposed)
└────────┘ └────────┘ └────────┘
```

**North-South:** Traffic entering/leaving the cluster (external → internal).

### Core Features

**1. Routing**

```yaml
# Example: Kong API Gateway configuration
routes:
  - name: catalog-route
    paths:
      - /api/v1/books
      - /api/v1/authors
    service: catalog-service

  - name: library-route
    paths:
      - /api/v1/bookcases
      - /api/v1/shelves
    service: library-service

  - name: circulation-route
    paths:
      - /api/v1/checkouts
    service: circulation-service
```

**Client calls:** `https://api.bibby.com/api/v1/books`
**Gateway routes to:** `http://catalog-service:8080/api/v1/books`

**2. Authentication & Authorization**

```javascript
// Gateway validates JWT before routing
function onRequest(request) {
  const token = request.headers['Authorization'];

  if (!validateJWT(token)) {
    return {
      status: 401,
      body: { error: 'Unauthorized' }
    };
  }

  // Add user context to request
  request.headers['X-User-ID'] = extractUserId(token);
  request.headers['X-User-Roles'] = extractRoles(token);

  // Forward to backend service
  return forwardRequest(request);
}
```

**Backend services don't need auth logic.** Trust headers from gateway.

**3. Rate Limiting**

```yaml
# Limit: 1000 requests/hour per user
plugins:
  - name: rate-limiting
    config:
      minute: null
      hour: 1000
      policy: local
      fault_tolerant: true
      hide_client_headers: false
      redis:
        host: redis
        port: 6379
```

**4. SSL Termination**

```
Client ─────HTTPS─────► API Gateway ─────HTTP─────► Backend Service
         (encrypted)                    (plaintext, internal network)
```

**Why:** Offload SSL/TLS processing from backend services. Simpler service code.

**5. Request/Response Transformation**

```javascript
// Add request headers
request.headers['X-Request-ID'] = generateUUID();
request.headers['X-Forwarded-For'] = clientIP;

// Transform response (legacy API → modern API)
response.body = {
  data: response.body,  // Wrap in envelope
  meta: {
    version: '2.0',
    timestamp: Date.now()
  }
};
```

**6. CORS Handling**

```yaml
plugins:
  - name: cors
    config:
      origins:
        - https://bibby.com
        - https://app.bibby.com
      methods:
        - GET
        - POST
        - PUT
        - DELETE
      headers:
        - Authorization
        - Content-Type
      exposed_headers:
        - X-Request-ID
      max_age: 3600
      credentials: true
```

---

## Deploying Kong API Gateway with Bibby

**Kong:** Open-source API gateway (CNCF project). Runs on Nginx.

### Install Kong on Kubernetes

**1. Install with Helm:**

```bash
helm repo add kong https://charts.konghq.com
helm repo update

helm install kong kong/kong \
  --namespace kong \
  --create-namespace \
  --set ingressController.installCRDs=false \
  --set proxy.type=LoadBalancer
```

**2. Configure Bibby routes:**

```yaml
apiVersion: configuration.konghq.com/v1
kind: KongPlugin
metadata:
  name: rate-limiting
  namespace: bibby
plugin: rate-limiting
config:
  minute: 100
  hour: 10000
  policy: local
---
apiVersion: configuration.konghq.com/v1
kind: KongPlugin
metadata:
  name: jwt-auth
  namespace: bibby
plugin: jwt
---
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: bibby-ingress
  namespace: bibby
  annotations:
    konghq.com/plugins: rate-limiting,jwt-auth
    konghq.com/strip-path: "true"
spec:
  ingressClassName: kong
  rules:
  - host: api.bibby.com
    http:
      paths:
      - path: /catalog
        pathType: Prefix
        backend:
          service:
            name: catalog-service
            port:
              number: 8080
      - path: /library
        pathType: Prefix
        backend:
          service:
            name: library-service
            port:
              number: 8080
      - path: /circulation
        pathType: Prefix
        backend:
          service:
            name: circulation-service
            port:
              number: 8080
```

**Client request flow:**

```
1. Client: GET https://api.bibby.com/catalog/books
2. Kong: Validate JWT token
3. Kong: Check rate limit (within quota?)
4. Kong: Strip /catalog prefix
5. Kong: Route to http://catalog-service:8080/books
6. Catalog Service: Process request, return response
7. Kong: Add CORS headers, return to client
```

### Custom Plugin: Request Logging

```lua
-- kong/plugins/bibby-logger/handler.lua
local RequestLogger = {}

function RequestLogger:access(config)
  local request_id = kong.request.get_header("X-Request-ID") or kong.uuid()
  kong.service.request.set_header("X-Request-ID", request_id)

  kong.log.info({
    request_id = request_id,
    method = kong.request.get_method(),
    path = kong.request.get_path(),
    client_ip = kong.client.get_forwarded_ip(),
    user_agent = kong.request.get_header("User-Agent")
  })
end

return RequestLogger
```

**Deploy:**
```bash
kubectl apply -f kong-custom-plugin.yaml
```

---

## What Is a Service Mesh?

**Definition:** Infrastructure layer for service-to-service communication. Handles routing, security, observability.

**Mental model:** The highway system between cities. Roads (network) exist, but the highway system adds: traffic lights (rate limiting), speed limits (quotas), cameras (observability), security checkpoints (mTLS).

### Traffic Pattern: East-West

```
┌────────────────────────────────────────────────┐
│              Kubernetes Cluster                │
│                                                │
│  Catalog Service ◄──mTLS──► Library Service   │
│       │                           │            │
│       │                           │            │
│  ┌────▼────┐                 ┌───▼─────┐      │
│  │ Sidecar │                 │ Sidecar │      │
│  │ Proxy   │◄────encrypted───│ Proxy   │      │
│  └─────────┘                 └─────────┘      │
│  (Envoy)                      (Envoy)         │
└────────────────────────────────────────────────┘
```

**East-West:** Traffic between services within the cluster.

**Sidecar pattern:** Each pod gets a proxy container injected alongside the application container.

### Core Features

**1. Automatic mTLS (Mutual TLS)**

**Without service mesh:**
```java
// In every service: configure SSL, manage certificates
@Configuration
public class SecurityConfig {
  @Bean
  public SSLContext sslContext() throws Exception {
    KeyStore keyStore = loadKeyStore();
    KeyStore trustStore = loadTrustStore();
    // 50 lines of boilerplate...
  }
}
```

**With service mesh:**
```yaml
# Zero application code changes. Mesh handles it.
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: default
  namespace: bibby
spec:
  mtls:
    mode: STRICT  # All traffic must be mTLS
```

**Result:** Catalog ↔ Library traffic automatically encrypted. Certificates rotated every 24 hours.

**2. Traffic Management**

**Canary deployment (10% of traffic to v2):**

```yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: catalog-service
spec:
  hosts:
  - catalog-service
  http:
  - match:
    - headers:
        x-canary:
          exact: "true"
    route:
    - destination:
        host: catalog-service
        subset: v2
  - route:
    - destination:
        host: catalog-service
        subset: v1
      weight: 90
    - destination:
        host: catalog-service
        subset: v2
      weight: 10
```

**3. Observability (Auto-instrumented)**

**Without service mesh:** Add OpenTelemetry to every service manually.

**With service mesh:** Metrics, traces, logs automatically collected from sidecar proxies.

```
# Automatic metrics (no code changes)
istio_requests_total{source_service="catalog", destination_service="library", response_code="200"}
istio_request_duration_milliseconds{source_service="catalog", destination_service="library", percentile="0.99"}
```

**4. Circuit Breaking**

```yaml
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: library-service
spec:
  host: library-service
  trafficPolicy:
    connectionPool:
      tcp:
        maxConnections: 100
      http:
        http1MaxPendingRequests: 50
        maxRequestsPerConnection: 2
    outlierDetection:
      consecutive5xxErrors: 5
      interval: 30s
      baseEjectionTime: 30s
      maxEjectionPercent: 50
```

**Translation:** If library-service returns 5 consecutive 5xx errors in 30 seconds, remove it from load balancer for 30 seconds.

---

## Deploying Istio Service Mesh with Bibby

**Istio:** Most popular service mesh (CNCF project). Uses Envoy proxy sidecars.

### Install Istio

```bash
# Download Istio
curl -L https://istio.io/downloadIstio | sh -
cd istio-1.20.0

# Install Istio on Kubernetes
istioctl install --set profile=demo -y

# Enable sidecar injection for bibby namespace
kubectl label namespace bibby istio-injection=enabled
```

**Redeploy Bibby pods:**
```bash
kubectl rollout restart deployment -n bibby

# Verify sidecars injected
kubectl get pods -n bibby

# Output:
NAME                        READY   STATUS
catalog-5d9f7c8b4-abc12     2/2     Running  ← 2 containers (app + sidecar)
library-7g3h9d2f6-def34     2/2     Running
```

### Configure mTLS for Bibby

```yaml
apiVersion: security.istio.io/v1beta1
kind: PeerAuthentication
metadata:
  name: bibby-mtls
  namespace: bibby
spec:
  mtls:
    mode: STRICT
---
apiVersion: security.istio.io/v1beta1
kind: AuthorizationPolicy
metadata:
  name: catalog-authz
  namespace: bibby
spec:
  selector:
    matchLabels:
      app: catalog-service
  action: ALLOW
  rules:
  - from:
    - source:
        principals:
        - "cluster.local/ns/bibby/sa/library-service"
    to:
    - operation:
        methods: ["GET"]
        paths: ["/api/v1/books/*"]
```

**Translation:** Only library-service (with specific service account) can call catalog-service's GET /api/v1/books/* endpoint.

### Traffic Shifting for Canary

**Scenario:** Deploy catalog-service v1.1, gradually shift traffic.

**1. Deploy v1.1:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: catalog-v1-1
spec:
  replicas: 2
  template:
    metadata:
      labels:
        app: catalog-service
        version: v1.1
    spec:
      containers:
      - name: catalog
        image: catalog:1.1
```

**2. Define subsets:**
```yaml
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: catalog
spec:
  host: catalog-service
  subsets:
  - name: v1
    labels:
      version: v1.0
  - name: v1-1
    labels:
      version: v1.1
```

**3. Shift traffic:**
```yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: catalog
spec:
  hosts:
  - catalog-service
  http:
  - route:
    - destination:
        host: catalog-service
        subset: v1
      weight: 50
    - destination:
        host: catalog-service
        subset: v1-1
      weight: 50  # 50/50 split
```

**Monitor error rates. If good, shift to 100%:**
```yaml
  - route:
    - destination:
        host: catalog-service
        subset: v1-1
      weight: 100
```

### Observability with Kiali

**Kiali:** Service mesh observability dashboard (visualizes traffic flow).

```bash
# Install Kiali
kubectl apply -f https://raw.githubusercontent.com/istio/istio/release-1.20/samples/addons/kiali.yaml

# Access dashboard
istioctl dashboard kiali
```

**Kiali shows:**
- Service graph (who calls whom)
- Traffic rates (requests/sec)
- Error rates (%)
- Latencies (p50, p95, p99)
- mTLS status (encrypted or not)

**Example visualization:**

```
┌──────────┐
│  Client  │
└────┬─────┘
     │ 100 req/s
     ▼
┌──────────┐
│ Kong GW  │
└────┬─────┘
     │ 100 req/s
     ├────────────────┬────────────────┐
     ▼                ▼                ▼
┌─────────┐     ┌──────────┐    ┌──────────┐
│ Catalog │────►│ Library  │    │  Circ.   │
│  60 r/s │     │  60 r/s  │    │  40 r/s  │
│  1% err │     │  0% err  │    │  0% err  │
└─────────┘     └──────────┘    └──────────┘
  p99: 45ms       p99: 120ms      p99: 30ms
      ▲                │
      └────────────────┘
         5 r/s (cache refresh)
```

---

## API Gateway vs Service Mesh: When to Use Each

| Feature | API Gateway | Service Mesh |
|---------|-------------|--------------|
| **Traffic** | North-South (external → internal) | East-West (internal ↔ internal) |
| **Use case** | Single entry point for clients | Service-to-service communication |
| **Authentication** | Validates external users (OAuth, API keys) | Validates services (mTLS, RBAC) |
| **Protocol** | HTTP/HTTPS, WebSocket | HTTP, gRPC, TCP |
| **Deployment** | Edge of cluster | Sidecar in every pod |
| **Overhead** | Low (one hop) | Medium (sidecar per pod) |
| **Examples** | Kong, AWS API Gateway, Ambassador | Istio, Linkerd, Consul Connect |

### Architecture Patterns

**Pattern 1: Gateway Only (Simple)**

```
Internet → API Gateway → Services (direct calls to each other)
```

**When to use:**
- Small number of services (< 5)
- Services trust each other (same team)
- No need for mTLS between services

**Pattern 2: Service Mesh Only (Internal)**

```
Internal clients → Services (mesh handles routing, mTLS, observability)
```

**When to use:**
- Only internal APIs (no public internet access)
- Focus on service-to-service security
- Already have external load balancer

**Pattern 3: Both (Recommended for Production)**

```
Internet → API Gateway → Service Mesh → Services
```

**Why both:**
- Gateway: Handles external auth, rate limiting, public API versioning
- Mesh: Handles internal mTLS, observability, traffic management

**Bibby example:**

```
1. Client calls: https://api.bibby.com/catalog/books
2. Kong API Gateway:
   - Validates JWT
   - Applies rate limit
   - Strips /catalog prefix
   - Routes to http://catalog-service/books
3. Istio sidecar (catalog pod):
   - Establishes mTLS connection to library-service sidecar
   - Applies retry policy (3 attempts)
   - Records metrics (latency, error rate)
4. Catalog service processes request
5. Catalog calls Library service (via mesh)
6. Response flows back through mesh → gateway → client
```

---

## Real-World War Story: Lyft's Envoy

**Background:** Lyft built microservices in 2015. 100+ services. **Nightmare:**
- Each service implemented own retry logic (inconsistent)
- No mTLS (services trusted network security)
- Debugging cross-service issues = impossible

**Solution (2016):** Built Envoy proxy. Deployed as sidecar to every service.

**Results:**
- Uniform retry/timeout policies across all services
- mTLS enabled for 100+ services in one deploy
- Centralized metrics (request rate, latency, errors)

**2017:** Open-sourced Envoy. Became foundation for Istio, Ambassador, etc.

**Lesson:** Service mesh complexity is worth it when you have 10+ services.

---

## Anti-Patterns to Avoid

### 1. Gateway Does Business Logic

**❌ BAD:**
```javascript
// In API Gateway
function onRequest(request) {
  if (request.path === '/books') {
    // Gateway queries database directly
    const books = database.query('SELECT * FROM books');
    return { status: 200, body: books };
  }
}
```

**Why bad:** Gateway becomes a monolith. Can't scale independently. Defeats microservices purpose.

**✅ GOOD:** Gateway routes to catalog-service. Service owns business logic.

### 2. Too Many Gateways

**❌ BAD:**
```
Client → Mobile Gateway → Web Gateway → Internal Gateway → Service
```

**Why bad:** Adds latency, operational complexity.

**✅ GOOD:** One gateway per environment (staging gateway, production gateway).

### 3. Service Mesh for < 5 Services

**❌ BAD:** Deploy Istio for 3 services.

**Why bad:** Overhead (sidecar per pod, control plane complexity) outweighs benefits.

**✅ GOOD:** Use service mesh when you have 10+ services or strict security requirements (compliance, multi-tenancy).

### 4. Not Monitoring Mesh Overhead

**Problem:** Sidecars add latency (1-5ms per hop) and memory (50-100MB per pod).

**Solution:**
```promql
# Monitor sidecar memory
sum(container_memory_usage_bytes{container="istio-proxy"}) by (pod)

# Monitor added latency
histogram_quantile(0.99, rate(istio_request_duration_milliseconds_bucket[5m]))
-
histogram_quantile(0.99, rate(http_server_requests_seconds_bucket[5m]))
```

**Acceptable:** < 5ms added latency, < 100MB per sidecar.

---

## Action Items

**For Bibby:**

1. **Deploy Kong API Gateway** (1 hour)
   ```bash
   helm install kong kong/kong --namespace kong --create-namespace
   kubectl apply -f bibby-ingress.yaml
   ```

2. **Add rate limiting** (15 min)
   - Create KongPlugin for rate-limiting
   - Annotate Ingress
   - Test: `ab -n 10000 -c 100 http://api.bibby.com/catalog/books`

3. **Deploy Istio** (optional, 2 hours)
   ```bash
   istioctl install --set profile=demo -y
   kubectl label namespace bibby istio-injection=enabled
   kubectl rollout restart deployment -n bibby
   ```

4. **Visualize with Kiali** (15 min)
   ```bash
   kubectl apply -f https://raw.githubusercontent.com/istio/istio/release-1.20/samples/addons/kiali.yaml
   istioctl dashboard kiali
   ```

**For your project:**

1. **Start with gateway** - Kong or AWS API Gateway
2. **Add authentication** - JWT validation at gateway
3. **Consider mesh when** - You have 10+ services or need mTLS
4. **Monitor overhead** - Latency and resource usage

---

## Further Reading

- **Kong Documentation:** https://docs.konghq.com/
- **Istio Documentation:** https://istio.io/latest/docs/
- **"Service Mesh Comparison"** by William Morgan (Linkerd creator)
- **Envoy Proxy:** https://www.envoyproxy.io/
- **AWS API Gateway:** https://aws.amazon.com/api-gateway/

---

## Next Section Preview

**Section 17: Patterns for Coordination** will teach you:
- Distributed transactions and sagas (deep dive)
- Choreography vs orchestration
- Event sourcing in depth
- Process managers and state machines
- Compensating transactions
- Idempotency keys and deduplication

We'll implement a complete saga for "Check out book → Send email → Update search index" with rollback logic.

Ready? Let's coordinate distributed workflows.

---

**Word count:** ~3,200 words
