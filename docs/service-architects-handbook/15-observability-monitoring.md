# Section 15: Observability & Monitoring

**Part V: Production Excellence**

---

## You Can't Fix What You Can't See

**3:42 AM. Pager goes off. Production is down.**

"What failed?"
"I don't know."

"When did it start failing?"
"No idea."

"What was the last deployment?"
"Maybe yesterday? Or Tuesday?"

"Are users affected?"
"Probably?"

**This is what happens without observability.**

With proper observability:
- **Metrics** show: CPU spiked to 100% at 3:39 AM
- **Logs** show: OutOfMemoryError in Bibby pod bibby-5d9f7c8b4-abc12
- **Traces** show: 95th percentile latency jumped from 50ms to 5000ms
- **Deployment history** shows: v1.3.2 deployed at 3:35 AM
- **Correlation:** New version has memory leak. Rollback to v1.3.1. Fixed in 3 minutes.

In this section, I'll show you how to instrument Bibby for **complete observability** using production-grade tools.

---

## The Three Pillars of Observability

### 1. Logs

**What:** Timestamped records of discrete events.

**Example:**
```
2025-01-18 14:23:45.123 INFO  BookService - Creating new book: Clean Code
2025-01-18 14:23:45.234 ERROR BookService - Failed to save book: Database connection timeout
```

**Use cases:**
- Debugging specific requests ("What happened to order #12345?")
- Auditing ("Who deleted user Alice?")
- Error investigation ("Why did this crash?")

**Challenges:**
- High volume (millions of lines/day)
- Storage costs ($$$)
- Finding the needle in the haystack

### 2. Metrics

**What:** Numerical measurements aggregated over time.

**Example:**
```
http_requests_total{method="GET", endpoint="/api/v1/books", status="200"} = 125643
http_request_duration_seconds{method="GET", endpoint="/api/v1/books", quantile="0.99"} = 0.234
```

**Use cases:**
- Dashboards (real-time system health)
- Alerting (CPU > 80% for 5 minutes)
- Capacity planning (traffic growing 20%/month)

**Challenges:**
- Cardinality explosion (too many unique metric combinations)
- What to measure?
- Storage retention (30 days? 1 year?)

### 3. Traces

**What:** End-to-end journey of a single request across multiple services.

**Example:**
```
Trace ID: abc123
Span 1: Catalog Service /search      (150ms)
  Span 2: Database query              (50ms)
  Span 3: Library Service /location   (80ms)
    Span 4: Database query            (75ms)  ← Bottleneck!
  Span 5: Cache update                (20ms)
```

**Use cases:**
- Performance debugging ("Why is search slow?")
- Dependency mapping ("Which services call which?")
- Identifying bottlenecks

**Challenges:**
- Overhead (tracing adds latency)
- Sampling (can't trace every request)
- Context propagation (trace IDs must flow through all services)

**Mental model:** Logs tell you **what happened**. Metrics tell you **how much/how often**. Traces tell you **where time was spent**.

---

## Instrumenting Bibby with Prometheus

**Prometheus:** Time-series database for metrics. Industry standard (CNCF graduated project).

**Architecture:**
```
┌──────────────┐      HTTP GET /metrics       ┌────────────────┐
│ Bibby Pods   │◄─────────────────────────────│  Prometheus    │
│ (exposes     │  (Prometheus scrapes every   │  (stores       │
│  metrics)    │   15 seconds)                │   metrics)     │
└──────────────┘                              └────────────────┘
                                                      │
                                                      │ PromQL queries
                                                      ▼
                                               ┌────────────────┐
                                               │    Grafana     │
                                               │  (dashboards)  │
                                               └────────────────┘
```

### Add Micrometer to Bibby

**1. Add dependencies to `pom.xml`:**

```xml
<dependencies>
    <!-- Existing dependencies... -->

    <!-- Micrometer for Prometheus -->
    <dependency>
        <groupId>io.micrometer</groupId>
        <artifactId>micrometer-registry-prometheus</artifactId>
    </dependency>

    <!-- Spring Boot Actuator (already mentioned in Section 11) -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
</dependencies>
```

**2. Configure `application.properties`:**

```properties
# Expose Prometheus metrics endpoint
management.endpoints.web.exposure.include=health,info,prometheus,metrics
management.metrics.export.prometheus.enabled=true

# Enable detailed metrics
management.metrics.enable.jvm=true
management.metrics.enable.process=true
management.metrics.enable.system=true
```

**3. Restart Bibby. Metrics now available at:**
```bash
curl http://localhost:8080/actuator/prometheus

# Output (sample):
# HELP jvm_memory_used_bytes The amount of used memory
# TYPE jvm_memory_used_bytes gauge
jvm_memory_used_bytes{area="heap",id="G1 Eden Space",} 4.194304E7
jvm_memory_used_bytes{area="heap",id="G1 Old Gen",} 1.2582912E7

# HELP http_server_requests_seconds
# TYPE http_server_requests_seconds summary
http_server_requests_seconds_count{method="GET",uri="/api/v1/books",status="200",} 1543.0
http_server_requests_seconds_sum{method="GET",uri="/api/v1/books",status="200",} 45.234
```

### Custom Metrics in BookService

**Add business metrics:**

```java
package com.penrose.bibby.library.book;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final Counter bookCreatedCounter;
    private final Counter bookCheckoutCounter;
    private final Timer bookSearchTimer;

    public BookService(
        BookRepository bookRepository,
        MeterRegistry meterRegistry
    ) {
        this.bookRepository = bookRepository;

        // Counter: How many books created
        this.bookCreatedCounter = Counter.builder("bibby.books.created")
            .description("Total books created")
            .tag("service", "bibby")
            .register(meterRegistry);

        // Counter: How many checkouts
        this.bookCheckoutCounter = Counter.builder("bibby.books.checkout")
            .description("Total books checked out")
            .tag("service", "bibby")
            .register(meterRegistry);

        // Timer: How long search takes
        this.bookSearchTimer = Timer.builder("bibby.books.search.duration")
            .description("Book search duration")
            .tag("service", "bibby")
            .register(meterRegistry);
    }

    public void createNewBook(BookRequestDTO request) {
        // Business logic...
        bookRepository.save(book);

        // Increment counter
        bookCreatedCounter.increment();
    }

    public void checkOutBook(BookEntity book) {
        book.setStatus(BookStatus.CHECKED_OUT);
        bookRepository.save(book);

        // Increment counter
        bookCheckoutCounter.increment();
    }

    public BookEntity findBookByTitle(String title) {
        // Measure search duration
        return bookSearchTimer.record(() -> {
            return bookRepository.findByTitleIgnoreCase(title);
        });
    }
}
```

**Metrics exposed:**
```
bibby_books_created_total 4523.0
bibby_books_checkout_total 892.0
bibby_books_search_duration_seconds_count 12456.0
bibby_books_search_duration_seconds_sum 234.567
```

### Deploy Prometheus to Kubernetes

**prometheus-config.yaml:**

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: prometheus-config
  namespace: monitoring
data:
  prometheus.yml: |
    global:
      scrape_interval: 15s
      evaluation_interval: 15s

    scrape_configs:
      - job_name: 'bibby'
        kubernetes_sd_configs:
        - role: pod
          namespaces:
            names:
            - bibby
        relabel_configs:
        - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_scrape]
          action: keep
          regex: true
        - source_labels: [__meta_kubernetes_pod_annotation_prometheus_io_path]
          action: replace
          target_label: __metrics_path__
          regex: (.+)
        - source_labels: [__address__, __meta_kubernetes_pod_annotation_prometheus_io_port]
          action: replace
          regex: ([^:]+)(?::\d+)?;(\d+)
          replacement: $1:$2
          target_label: __address__
```

**prometheus-deployment.yaml:**

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: prometheus
  namespace: monitoring
spec:
  replicas: 1
  selector:
    matchLabels:
      app: prometheus
  template:
    metadata:
      labels:
        app: prometheus
    spec:
      containers:
      - name: prometheus
        image: prom/prometheus:v2.45.0
        args:
        - '--config.file=/etc/prometheus/prometheus.yml'
        - '--storage.tsdb.path=/prometheus'
        - '--storage.tsdb.retention.time=30d'
        ports:
        - containerPort: 9090
        volumeMounts:
        - name: config
          mountPath: /etc/prometheus
        - name: storage
          mountPath: /prometheus
      volumes:
      - name: config
        configMap:
          name: prometheus-config
      - name: storage
        emptyDir: {}
```

**Annotate Bibby pods for scraping:**

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: bibby
spec:
  template:
    metadata:
      annotations:
        prometheus.io/scrape: "true"
        prometheus.io/path: "/actuator/prometheus"
        prometheus.io/port: "8080"
    spec:
      # ... rest of spec
```

**Deploy:**
```bash
kubectl create namespace monitoring
kubectl apply -f prometheus-config.yaml
kubectl apply -f prometheus-deployment.yaml

# Access Prometheus UI
kubectl port-forward -n monitoring svc/prometheus 9090:9090
# Open http://localhost:9090
```

---

## Visualizing with Grafana

**Grafana:** Visualization platform for metrics.

### Deploy Grafana to Kubernetes

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: grafana
  namespace: monitoring
spec:
  replicas: 1
  selector:
    matchLabels:
      app: grafana
  template:
    metadata:
      labels:
        app: grafana
    spec:
      containers:
      - name: grafana
        image: grafana/grafana:10.0.0
        ports:
        - containerPort: 3000
        env:
        - name: GF_SECURITY_ADMIN_PASSWORD
          value: "admin"  # Change in production!
        volumeMounts:
        - name: storage
          mountPath: /var/lib/grafana
      volumes:
      - name: storage
        emptyDir: {}
---
apiVersion: v1
kind: Service
metadata:
  name: grafana
  namespace: monitoring
spec:
  selector:
    app: grafana
  ports:
  - port: 3000
    targetPort: 3000
  type: LoadBalancer
```

**Access Grafana:**
```bash
kubectl apply -f grafana-deployment.yaml
kubectl port-forward -n monitoring svc/grafana 3000:3000
# Open http://localhost:3000 (admin/admin)
```

### Create Bibby Dashboard

**1. Add Prometheus datasource:**
- Settings → Data Sources → Add Prometheus
- URL: `http://prometheus:9090`

**2. Create dashboard with panels:**

**Panel 1: Books Created (Counter)**
```promql
rate(bibby_books_created_total[5m])
```
**Visualization:** Graph
**Legend:** Books created per second (5-minute average)

**Panel 2: Checkout Rate**
```promql
rate(bibby_books_checkout_total[1h])
```
**Visualization:** Stat
**Legend:** Books checked out per hour

**Panel 3: Search Latency (P95)**
```promql
histogram_quantile(0.95,
  rate(bibby_books_search_duration_seconds_bucket[5m])
)
```
**Visualization:** Graph
**Legend:** 95th percentile search latency

**Panel 4: JVM Memory Usage**
```promql
jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"} * 100
```
**Visualization:** Gauge
**Legend:** Heap memory usage (%)

**Panel 5: HTTP Requests by Status Code**
```promql
sum by (status) (rate(http_server_requests_seconds_count[5m]))
```
**Visualization:** Graph (stacked)
**Legend:** Requests/sec by status (200, 404, 500)

**Panel 6: Error Rate**
```promql
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
/
sum(rate(http_server_requests_seconds_count[5m])) * 100
```
**Visualization:** Stat with thresholds (green < 1%, yellow < 5%, red > 5%)
**Legend:** Error rate (%)

**Save dashboard as JSON:**
```json
{
  "dashboard": {
    "title": "Bibby Service Metrics",
    "panels": [ /* ... */ ],
    "refresh": "10s",
    "time": {
      "from": "now-1h",
      "to": "now"
    }
  }
}
```

---

## Centralized Logging with ELK Stack

**ELK:** Elasticsearch + Logstash + Kibana (or Fluent Bit instead of Logstash = EFK)

**Architecture:**
```
┌─────────────┐
│ Bibby Pods  │──┐
│ (logs to    │  │
│  stdout)    │  │
└─────────────┘  │
                 ├──► ┌──────────────┐      ┌─────────────────┐
┌─────────────┐  │     │  Fluent Bit  │─────►│ Elasticsearch   │
│ Other Pods  │──┘     │ (collects    │      │ (stores logs)   │
└─────────────┘        │  logs)       │      └─────────────────┘
                       └──────────────┘               │
                                                      │
                                               ┌──────▼──────┐
                                               │   Kibana    │
                                               │ (search UI) │
                                               └─────────────┘
```

### Configure Structured Logging in Bibby

**Add Logback JSON encoder:**

```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

**`src/main/resources/logback-spring.xml`:**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <customFields>{"application":"bibby","environment":"${ENVIRONMENT:-dev}"}</customFields>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE" />
    </root>
</configuration>
```

**Now logs are JSON:**
```json
{
  "@timestamp": "2025-01-18T14:23:45.123Z",
  "level": "INFO",
  "logger_name": "com.penrose.bibby.library.book.BookService",
  "message": "Creating new book: Clean Code",
  "thread_name": "http-nio-8080-exec-1",
  "application": "bibby",
  "environment": "production",
  "trace_id": "abc123",  // ← Added by tracing (next section)
  "span_id": "xyz789"
}
```

### Deploy Fluent Bit DaemonSet

**Fluent Bit runs on every Kubernetes node, collects logs from all pods.**

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: fluent-bit-config
  namespace: logging
data:
  fluent-bit.conf: |
    [SERVICE]
        Flush         5
        Log_Level     info

    [INPUT]
        Name              tail
        Path              /var/log/containers/*bibby*.log
        Parser            docker
        Tag               kube.*
        Refresh_Interval  5
        Mem_Buf_Limit     5MB

    [FILTER]
        Name                kubernetes
        Match               kube.*
        Kube_URL            https://kubernetes.default.svc:443
        Kube_CA_File        /var/run/secrets/kubernetes.io/serviceaccount/ca.crt
        Kube_Token_File     /var/run/secrets/kubernetes.io/serviceaccount/token

    [OUTPUT]
        Name  es
        Match *
        Host  elasticsearch
        Port  9200
        Index bibby-logs
        Type  _doc
---
apiVersion: apps/v1
kind: DaemonSet
metadata:
  name: fluent-bit
  namespace: logging
spec:
  selector:
    matchLabels:
      app: fluent-bit
  template:
    metadata:
      labels:
        app: fluent-bit
    spec:
      containers:
      - name: fluent-bit
        image: fluent/fluent-bit:2.1
        volumeMounts:
        - name: varlog
          mountPath: /var/log
        - name: config
          mountPath: /fluent-bit/etc/
      volumes:
      - name: varlog
        hostPath:
          path: /var/log
      - name: config
        configMap:
          name: fluent-bit-config
```

### Query Logs in Kibana

**Example queries:**

**Find all errors:**
```
level:ERROR AND application:bibby
```

**Find logs for specific trace:**
```
trace_id:"abc123"
```

**Find slow database queries:**
```
message:*"Database query"* AND duration:>1000
```

---

## Distributed Tracing with Jaeger

**Jaeger:** Open-source distributed tracing (from Uber).

**Why tracing matters:** When a request touches 5 services, logs alone can't show the full picture.

### Add OpenTelemetry to Bibby

**1. Add dependencies:**

```xml
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-api</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry.instrumentation</groupId>
    <artifactId>opentelemetry-spring-boot-starter</artifactId>
    <version>1.32.0-alpha</version>
</dependency>
```

**2. Configure in `application.properties`:**

```properties
# OpenTelemetry configuration
otel.service.name=bibby-catalog-service
otel.traces.exporter=jaeger
otel.exporter.jaeger.endpoint=http://jaeger-collector:14250
otel.metrics.exporter=prometheus
```

**3. Automatic instrumentation:**

Spring Boot auto-instruments:
- HTTP requests (incoming/outgoing)
- Database queries
- Redis calls
- Kafka messages

**No code changes needed!**

### Deploy Jaeger

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: jaeger
  namespace: tracing
spec:
  replicas: 1
  selector:
    matchLabels:
      app: jaeger
  template:
    metadata:
      labels:
        app: jaeger
    spec:
      containers:
      - name: jaeger
        image: jaegertracing/all-in-one:1.50
        ports:
        - containerPort: 16686  # UI
        - containerPort: 14250  # gRPC collector
        env:
        - name: COLLECTOR_OTLP_ENABLED
          value: "true"
---
apiVersion: v1
kind: Service
metadata:
  name: jaeger-collector
  namespace: tracing
spec:
  selector:
    app: jaeger
  ports:
  - name: grpc
    port: 14250
    targetPort: 14250
---
apiVersion: v1
kind: Service
metadata:
  name: jaeger-ui
  namespace: tracing
spec:
  selector:
    app: jaeger
  ports:
  - port: 16686
    targetPort: 16686
  type: LoadBalancer
```

**Access Jaeger UI:**
```bash
kubectl apply -f jaeger-deployment.yaml
kubectl port-forward -n tracing svc/jaeger-ui 16686:16686
# Open http://localhost:16686
```

### Example Trace: Book Search

**Scenario:** User searches for "Clean Code". Request touches Catalog Service → Library Service → Database (2x).

**Trace in Jaeger:**

```
Trace abc123 (Total: 234ms)
├─ Span 1: HTTP GET /api/v1/books/search (Catalog Service) [234ms]
│  ├─ Span 2: BookService.searchByTitle() [220ms]
│  │  ├─ Span 3: SQL SELECT FROM books WHERE title LIKE... [45ms]
│  │  ├─ Span 4: HTTP GET library-service/locations/7 (Library Service) [160ms]
│  │  │  ├─ Span 5: SQL SELECT FROM shelves WHERE id=7 [75ms] ← Slow!
│  │  │  └─ Span 6: SQL SELECT FROM bookcases WHERE id=3 [50ms]
│  │  └─ Span 7: Redis SET location:7 (cache update) [15ms]
│  └─ Span 8: JSON serialization [14ms]
```

**Analysis:** Span 5 (shelf query) is slow (75ms). Investigate database indexing.

---

## Alerting with Prometheus Alertmanager

**Alert rules:** Define when to notify humans.

**prometheus-alerts.yaml:**

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: prometheus-alerts
  namespace: monitoring
data:
  alerts.yml: |
    groups:
    - name: bibby
      interval: 30s
      rules:
      # Alert: High error rate
      - alert: HighErrorRate
        expr: |
          (
            sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
            /
            sum(rate(http_server_requests_seconds_count[5m]))
          ) > 0.05
        for: 5m
        labels:
          severity: critical
          service: bibby
        annotations:
          summary: "High error rate on Bibby service"
          description: "Error rate is {{ $value | humanizePercentage }} (threshold: 5%)"

      # Alert: High latency (p99 > 500ms)
      - alert: HighLatency
        expr: |
          histogram_quantile(0.99,
            rate(http_server_requests_seconds_bucket[5m])
          ) > 0.5
        for: 10m
        labels:
          severity: warning
          service: bibby
        annotations:
          summary: "High latency on Bibby service"
          description: "P99 latency is {{ $value }}s (threshold: 0.5s)"

      # Alert: Pod down
      - alert: PodDown
        expr: up{job="bibby"} == 0
        for: 1m
        labels:
          severity: critical
          service: bibby
        annotations:
          summary: "Bibby pod is down"
          description: "Pod {{ $labels.pod }} is not reachable"

      # Alert: High memory usage
      - alert: HighMemoryUsage
        expr: |
          (jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) > 0.9
        for: 5m
        labels:
          severity: warning
          service: bibby
        annotations:
          summary: "High memory usage on Bibby"
          description: "Heap usage is {{ $value | humanizePercentage }}"
```

**Alertmanager configuration:**

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: alertmanager-config
  namespace: monitoring
data:
  alertmanager.yml: |
    global:
      slack_api_url: 'https://hooks.slack.com/services/YOUR/WEBHOOK/URL'

    route:
      receiver: 'default'
      group_by: ['alertname', 'service']
      group_wait: 10s
      group_interval: 10s
      repeat_interval: 12h
      routes:
      - match:
          severity: critical
        receiver: 'pagerduty'
      - match:
          severity: warning
        receiver: 'slack'

    receivers:
    - name: 'default'
      slack_configs:
      - channel: '#alerts'
        title: '{{ .GroupLabels.alertname }}'
        text: '{{ range .Alerts }}{{ .Annotations.description }}{{ end }}'

    - name: 'slack'
      slack_configs:
      - channel: '#bibby-warnings'
        title: '⚠️ {{ .GroupLabels.alertname }}'
        text: '{{ range .Alerts }}{{ .Annotations.summary }}{{ end }}'

    - name: 'pagerduty'
      pagerduty_configs:
      - service_key: 'YOUR_PAGERDUTY_KEY'
```

---

## Real-World War Story: Target's 2013 Breach

**Incident:** Hackers stole 40 million credit card numbers.

**Monitoring failure:** Security team had alerts configured. **Alerts fired. No one responded.**

**Root cause:** Alert fatigue. Team received 1000+ alerts/day, most false positives. Ignored them all.

**Lessons:**
1. **Alert on symptoms, not causes** - Alert on "error rate > 5%", not "disk 80% full" (latter may not affect users)
2. **Every alert should be actionable** - If you can't fix it now, it's not an alert
3. **Tune thresholds** - 1000 alerts/day = 0 alerts/day (all ignored)
4. **Escalation policy** - Critical alerts page on-call, warnings go to Slack

---

## Action Items

**For Bibby:**

1. **Add Prometheus metrics** (30 min)
   - Add Micrometer dependency
   - Expose `/actuator/prometheus` endpoint
   - Deploy Prometheus to Minikube
   - Verify metrics scraped

2. **Create Grafana dashboard** (1 hour)
   - Deploy Grafana
   - Add Prometheus datasource
   - Create dashboard with 6 panels
   - Save dashboard JSON

3. **Add structured logging** (30 min)
   - Add Logstash encoder
   - Configure logback-spring.xml
   - Verify JSON logs in console

4. **Deploy Jaeger** (optional, 1 hour)
   - Add OpenTelemetry dependencies
   - Deploy Jaeger
   - Generate traffic, view traces

**For your project:**

1. **Instrument metrics** - Business metrics + system metrics
2. **Create dashboards** - One dashboard per service
3. **Configure alerts** - Start with error rate + latency
4. **Centralize logs** - ELK or CloudWatch or Datadog
5. **Add tracing** - For distributed systems only

---

## Further Reading

- **"Observability Engineering"** by Charity Majors (Honeycomb)
- **Prometheus Docs:** https://prometheus.io/docs/
- **Grafana Dashboards:** https://grafana.com/grafana/dashboards/
- **OpenTelemetry:** https://opentelemetry.io/
- **Google SRE Book:** Chapter 6 (Monitoring Distributed Systems)

---

**Word count:** ~4,500 words
