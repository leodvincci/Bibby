# Section 14: CI/CD for Microservices

**Part IV: Infrastructure & Deployment**

---

## Manual Deploys Don't Scale

**Friday, 4:47 PM. Production is down. Critical bug fix ready.**

**Manual deploy process:**
1. Developer builds JAR locally: `mvn clean package` (3 minutes)
2. Copies JAR to jump box via SCP (2 minutes)
3. SSHs into production server
4. Stops Tomcat (`sudo systemctl stop tomcat`)
5. Replaces old JAR with new JAR
6. Starts Tomcat (`sudo systemctl start tomcat`)
7. Prays it works
8. Realizes they forgot to update database schema
9. Application crashes
10. **Total time: 30 minutes + 2 hours of debugging**

**With CI/CD:**
1. Developer pushes fix to Git
2. Pipeline automatically: builds, tests, creates Docker image, scans for vulnerabilities, deploys to Kubernetes
3. Kubernetes performs rolling update (zero downtime)
4. **Total time: 5 minutes**

In this section, I'll show you how to automate **everything** from code commit to production deployment using Bibby as the example.

---

## CI/CD Fundamentals

### Continuous Integration (CI)

**Definition:** Automatically build and test code on every commit.

**Goals:**
- Catch bugs early (before they reach production)
- Ensure code always compiles
- Maintain fast feedback loop (< 10 minutes)
- Prevent "it works on my machine" issues

**Key practices:**
- Commit to main branch frequently (at least daily)
- Every commit triggers automated build + tests
- Build must be fast (< 10 minutes ideal)
- Fix broken builds immediately (priority #1)

### Continuous Delivery (CD)

**Definition:** Code is **always** in a deployable state. Manual approval required for production deploy.

**Example:** Commit → CI builds + tests → Artifact stored → **[Manual approval]** → Deploy to production

### Continuous Deployment (CD)

**Definition:** Code automatically deployed to production on every commit (no manual approval).

**Example:** Commit → CI builds + tests → Artifact stored → **[Automatic deploy]** → Production

**When to use each:**
- **Continuous Delivery:** Regulated industries (finance, healthcare), require manual approval
- **Continuous Deployment:** Fast-moving startups, internal tools, microservices with extensive testing

---

## The CI/CD Pipeline for Bibby

**From `pom.xml:30`, Bibby uses:**
- Java 17
- Spring Boot 3.5.7
- Maven for builds

**Our pipeline will:**
1. Checkout code
2. Run unit tests
3. Run integration tests (with PostgreSQL)
4. Build Docker image
5. Scan image for vulnerabilities
6. Push image to registry
7. Deploy to Kubernetes (staging)
8. Run smoke tests
9. **(Optional)** Deploy to production

**Tools we'll use:**
- **CI Platform:** GitHub Actions (could be Jenkins, GitLab CI, CircleCI)
- **Container Registry:** Docker Hub (could be AWS ECR, GCR)
- **Deployment:** kubectl (could be ArgoCD, Flux)

---

## GitHub Actions Pipeline for Bibby

### Complete Workflow File

**`.github/workflows/ci-cd.yml`:**

```yaml
name: Bibby CI/CD Pipeline

on:
  push:
    branches:
      - main
      - develop
  pull_request:
    branches:
      - main

env:
  DOCKER_IMAGE: bibby
  DOCKER_TAG: ${{ github.sha }}

jobs:
  # ──────────────────────────────────────────────────────────
  # Job 1: Build and Test
  # ──────────────────────────────────────────────────────────
  build-and-test:
    runs-on: ubuntu-latest

    services:
      postgres:
        image: postgres:15-alpine
        env:
          POSTGRES_DB: amigos
          POSTGRES_USER: amigoscode
          POSTGRES_PASSWORD: password
        ports:
          - 5432:5432
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: 'temurin'
          java-version: '17'
          cache: 'maven'

      - name: Run unit tests
        run: mvn test

      - name: Run integration tests
        env:
          SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/amigos
          SPRING_DATASOURCE_USERNAME: amigoscode
          SPRING_DATASOURCE_PASSWORD: password
        run: mvn verify -Pintegration-tests

      - name: Build JAR
        run: mvn clean package -DskipTests

      - name: Upload JAR artifact
        uses: actions/upload-artifact@v4
        with:
          name: bibby-jar
          path: target/Bibby-0.0.1-SNAPSHOT.jar
          retention-days: 7

  # ──────────────────────────────────────────────────────────
  # Job 2: Build and Scan Docker Image
  # ──────────────────────────────────────────────────────────
  docker-build-and-scan:
    runs-on: ubuntu-latest
    needs: build-and-test  # Only run if tests pass

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Log in to Docker Hub
        uses: docker/login-action@v3
        with:
          username: ${{ secrets.DOCKER_USERNAME }}
          password: ${{ secrets.DOCKER_PASSWORD }}

      - name: Build Docker image
        uses: docker/build-push-action@v5
        with:
          context: .
          push: false  # Don't push yet (scan first)
          load: true
          tags: |
            ${{ env.DOCKER_IMAGE }}:${{ env.DOCKER_TAG }}
            ${{ env.DOCKER_IMAGE }}:latest
          cache-from: type=gha
          cache-to: type=gha,mode=max

      - name: Scan image with Trivy
        uses: aquasecurity/trivy-action@master
        with:
          image-ref: ${{ env.DOCKER_IMAGE }}:${{ env.DOCKER_TAG }}
          format: 'sarif'
          output: 'trivy-results.sarif'
          severity: 'CRITICAL,HIGH'
          exit-code: 1  # Fail if critical/high vulnerabilities found

      - name: Upload Trivy scan results
        uses: github/codeql-action/upload-sarif@v3
        if: always()
        with:
          sarif_file: 'trivy-results.sarif'

      - name: Push Docker image
        if: github.event_name == 'push' && github.ref == 'refs/heads/main'
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: |
            ${{ secrets.DOCKER_USERNAME }}/${{ env.DOCKER_IMAGE }}:${{ env.DOCKER_TAG }}
            ${{ secrets.DOCKER_USERNAME }}/${{ env.DOCKER_IMAGE }}:latest
          cache-from: type=gha

  # ──────────────────────────────────────────────────────────
  # Job 3: Deploy to Staging (Kubernetes)
  # ──────────────────────────────────────────────────────────
  deploy-staging:
    runs-on: ubuntu-latest
    needs: docker-build-and-scan
    if: github.event_name == 'push' && github.ref == 'refs/heads/main'

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Configure kubectl
        uses: azure/k8s-set-context@v3
        with:
          method: kubeconfig
          kubeconfig: ${{ secrets.KUBE_CONFIG_STAGING }}

      - name: Update deployment image
        run: |
          kubectl set image deployment/bibby \
            bibby=${{ secrets.DOCKER_USERNAME }}/${{ env.DOCKER_IMAGE }}:${{ env.DOCKER_TAG }} \
            -n bibby-staging

      - name: Wait for rollout
        run: |
          kubectl rollout status deployment/bibby -n bibby-staging --timeout=5m

      - name: Run smoke tests
        run: |
          STAGING_URL=$(kubectl get svc bibby-service -n bibby-staging -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
          curl -f http://$STAGING_URL:8080/actuator/health || exit 1

  # ──────────────────────────────────────────────────────────
  # Job 4: Deploy to Production (Manual Approval)
  # ──────────────────────────────────────────────────────────
  deploy-production:
    runs-on: ubuntu-latest
    needs: deploy-staging
    if: github.event_name == 'push' && github.ref == 'refs/heads/main'
    environment: production  # Requires manual approval in GitHub

    steps:
      - name: Checkout code
        uses: actions/checkout@v4

      - name: Configure kubectl
        uses: azure/k8s-set-context@v3
        with:
          method: kubeconfig
          kubeconfig: ${{ secrets.KUBE_CONFIG_PRODUCTION }}

      - name: Update deployment image
        run: |
          kubectl set image deployment/bibby \
            bibby=${{ secrets.DOCKER_USERNAME }}/${{ env.DOCKER_IMAGE }}:${{ env.DOCKER_TAG }} \
            -n bibby-production

      - name: Wait for rollout
        run: |
          kubectl rollout status deployment/bibby -n bibby-production --timeout=10m

      - name: Verify deployment
        run: |
          PROD_URL=$(kubectl get svc bibby-service -n bibby-production -o jsonpath='{.status.loadBalancer.ingress[0].ip}')
          curl -f http://$PROD_URL:8080/actuator/health || exit 1

      - name: Notify Slack
        uses: slackapi/slack-github-action@v1
        with:
          webhook-url: ${{ secrets.SLACK_WEBHOOK }}
          payload: |
            {
              "text": "✅ Bibby ${{ github.sha }} deployed to production"
            }
```

### Secrets Configuration

**In GitHub repo: Settings → Secrets and variables → Actions:**

```
DOCKER_USERNAME          = your-dockerhub-username
DOCKER_PASSWORD          = your-dockerhub-token
KUBE_CONFIG_STAGING      = base64-encoded kubeconfig for staging cluster
KUBE_CONFIG_PRODUCTION   = base64-encoded kubeconfig for production cluster
SLACK_WEBHOOK            = https://hooks.slack.com/services/...
```

**Generate kubeconfig secret:**
```bash
# Get kubeconfig
kubectl config view --flatten --minify > kubeconfig.yaml

# Base64 encode
cat kubeconfig.yaml | base64 | pbcopy  # macOS
cat kubeconfig.yaml | base64 -w0       # Linux

# Paste into GitHub secret
```

---

## Testing Strategies

### 1. Unit Tests

**Test individual components in isolation.**

**Example (hypothetical test for Bibby):**

```java
// src/test/java/com/penrose/bibby/library/book/BookServiceTest.java
@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private AuthorRepository authorRepository;

    @InjectMocks
    private BookService bookService;

    @Test
    void shouldCreateNewBookWithNewAuthor() {
        // Given
        BookRequestDTO request = new BookRequestDTO(
            "Clean Code", "Robert", "Martin"
        );
        when(bookRepository.findByTitle("Clean Code")).thenReturn(null);
        when(authorRepository.findByFirstNameAndLastName("Robert", "Martin"))
            .thenReturn(null);

        // When
        bookService.createNewBook(request);

        // Then
        verify(authorRepository, times(1)).save(any(AuthorEntity.class));
        verify(bookRepository, times(1)).save(any(BookEntity.class));
    }

    @Test
    void shouldNotCreateDuplicateAuthor() {
        // Given
        AuthorEntity existingAuthor = new AuthorEntity("Robert", "Martin");
        when(authorRepository.findByFirstNameAndLastName("Robert", "Martin"))
            .thenReturn(existingAuthor);

        // When
        bookService.createNewBook(new BookRequestDTO(
            "Clean Architecture", "Robert", "Martin"
        ));

        // Then
        verify(authorRepository, never()).save(any(AuthorEntity.class));
        verify(bookRepository, times(1)).save(any(BookEntity.class));
    }
}
```

**Run with:**
```bash
mvn test
```

### 2. Integration Tests

**Test components with real dependencies (database, external services).**

**Example:**

```java
// src/test/java/com/penrose/bibby/library/book/BookServiceIntegrationTest.java
@SpringBootTest
@Testcontainers
class BookServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
        .withDatabaseName("amigos")
        .withUsername("test")
        .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private BookService bookService;

    @Autowired
    private BookRepository bookRepository;

    @Test
    void shouldPersistBookWithAuthor() {
        // Given
        BookRequestDTO request = new BookRequestDTO(
            "Domain-Driven Design", "Eric", "Evans"
        );

        // When
        bookService.createNewBook(request);

        // Then
        BookEntity book = bookRepository.findByTitle("Domain-Driven Design");
        assertThat(book).isNotNull();
        assertThat(book.getAuthors()).hasSize(1);
        assertThat(book.getAuthors().iterator().next().getFirstName()).isEqualTo("Eric");
    }
}
```

**Testcontainers:** Spins up real PostgreSQL in Docker for tests.

**Run with:**
```bash
mvn verify -Pintegration-tests
```

### 3. End-to-End Tests

**Test entire application flow (user perspective).**

**Example (using REST Assured):**

```java
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class BookE2ETest {

    @LocalServerPort
    private int port;

    @Test
    void shouldCreateAndRetrieveBook() {
        // Create book
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "title": "Microservices Patterns",
                  "firstName": "Chris",
                  "lastName": "Richardson"
                }
                """)
        .when()
            .post("http://localhost:" + port + "/api/v1/books")
        .then()
            .statusCode(201);

        // Retrieve book
        given()
            .queryParam("title", "Microservices Patterns")
        .when()
            .get("http://localhost:" + port + "/api/v1/books/search")
        .then()
            .statusCode(200)
            .body("title", equalTo("Microservices Patterns"))
            .body("authors[0].firstName", equalTo("Chris"));
    }
}
```

### Test Pyramid

```
        /\
       /  \     E2E Tests (10%)
      /────\    - Slow, brittle, expensive
     /      \   - Test critical user journeys
    /────────\
   / Integra- \ Integration Tests (20%)
  /────────────\  - Test with real DB, message brokers
 /   Unit Tests \ Unit Tests (70%)
/────────────────\ - Fast, reliable, cheap
```

**Golden rule:** Most tests should be unit tests. Minimize E2E tests.

---

## Docker Image Optimization for CI

**Problem:** Building Docker images in CI is slow.

### Layer Caching

**GitHub Actions caches Docker layers between builds.**

```yaml
- name: Build Docker image
  uses: docker/build-push-action@v5
  with:
    context: .
    push: false
    cache-from: type=gha  # ← Load cache from GitHub Actions
    cache-to: type=gha,mode=max  # ← Save cache
```

**First build:** 5 minutes (downloads all Maven dependencies)
**Subsequent builds (code change only):** 30 seconds (reuses cached dependency layer)

### Multi-Stage Build Optimization

**From Section 12, our Dockerfile has two stages:**

```dockerfile
# Stage 1: Build (heavy)
FROM maven:3.9-eclipse-temurin-17 AS builder
# ... build JAR

# Stage 2: Runtime (lightweight)
FROM eclipse-temurin:17-jre-alpine
COPY --from=builder /build/target/*.jar app.jar
```

**CI only pushes Stage 2 (runtime image).** Stage 1 (build artifacts) discarded.

**Result:** Image pushed to registry is 245MB, not 800MB.

---

## GitOps: Infrastructure as Code

**Traditional deployment:**
```
Developer → kubectl apply → Kubernetes cluster
```

**Problem:** No audit trail. No rollback. Manual.

**GitOps approach:**
```
Developer → Git commit → ArgoCD watches Git → ArgoCD applies to Kubernetes
```

**Benefits:**
- ✅ Git is source of truth (declarative)
- ✅ Audit trail (Git history)
- ✅ Rollback (git revert)
- ✅ Pull request for production changes

### ArgoCD for Bibby

**1. Install ArgoCD:**
```bash
kubectl create namespace argocd
kubectl apply -n argocd -f https://raw.githubusercontent.com/argoproj/argo-cd/stable/manifests/install.yaml

# Access UI
kubectl port-forward svc/argocd-server -n argocd 8080:443
```

**2. Create Application manifest:**

```yaml
# argocd/bibby-app.yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: bibby-production
  namespace: argocd
spec:
  project: default
  source:
    repoURL: https://github.com/yourname/bibby
    targetRevision: main
    path: k8s/overlays/production  # Kustomize overlays
  destination:
    server: https://kubernetes.default.svc
    namespace: bibby-production
  syncPolicy:
    automated:
      prune: true    # Delete resources removed from Git
      selfHeal: true # Sync if someone manually changes cluster
    syncOptions:
    - CreateNamespace=true
```

**3. Deploy with ArgoCD:**
```bash
kubectl apply -f argocd/bibby-app.yaml
```

**Now:** Every commit to `main` that changes `k8s/overlays/production/` automatically deploys to production.

**ArgoCD UI shows:**
- Current state vs desired state
- Sync status
- Rollback buttons
- Health checks

---

## Blue-Green Deployments

**Goal:** Zero-downtime deployments with instant rollback.

**Concept:** Run two identical production environments (Blue and Green). Switch traffic between them.

### Kubernetes Implementation

**1. Blue deployment (currently serving traffic):**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: bibby-blue
  labels:
    version: blue
spec:
  replicas: 3
  selector:
    matchLabels:
      app: bibby
      version: blue
  template:
    metadata:
      labels:
        app: bibby
        version: blue
    spec:
      containers:
      - name: bibby
        image: bibby:1.0  # Current version
```

**2. Green deployment (new version, not serving traffic yet):**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: bibby-green
  labels:
    version: green
spec:
  replicas: 3
  selector:
    matchLabels:
      app: bibby
      version: green
  template:
    metadata:
      labels:
        app: bibby
        version: green
    spec:
      containers:
      - name: bibby
        image: bibby:1.1  # New version
```

**3. Service (routes to Blue initially):**
```yaml
apiVersion: v1
kind: Service
metadata:
  name: bibby-service
spec:
  selector:
    app: bibby
    version: blue  # ← Traffic goes to Blue
  ports:
  - port: 80
    targetPort: 8080
```

**4. Deploy Green, test, then switch:**
```bash
# Deploy Green
kubectl apply -f bibby-green.yaml

# Test Green internally (port-forward)
kubectl port-forward deployment/bibby-green 9090:8080
curl http://localhost:9090/actuator/health

# Switch traffic to Green
kubectl patch svc bibby-service -p '{"spec":{"selector":{"version":"green"}}}'

# Instant cutover! All traffic now goes to Green.

# If there's a problem, instant rollback:
kubectl patch svc bibby-service -p '{"spec":{"selector":{"version":"blue"}}}'
```

**Trade-offs:**
- ✅ Instant rollback (change label selector)
- ✅ Test new version in production environment (without user traffic)
- ❌ Double resource usage (running both Blue and Green)
- ❌ Database migrations tricky (both versions must work with same schema)

---

## Canary Releases

**Goal:** Gradually roll out new version to subset of users. Monitor for errors.

**Concept:** Route 10% of traffic to v1.1, 90% to v1.0. If metrics look good, increase to 50/50, then 100%.

### Kubernetes Implementation (with Istio)

**1. DestinationRule (define subsets):**
```yaml
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: bibby
spec:
  host: bibby-service
  subsets:
  - name: v1
    labels:
      version: "1.0"
  - name: v2
    labels:
      version: "1.1"
```

**2. VirtualService (traffic split):**
```yaml
apiVersion: networking.istio.io/v1beta1
kind: VirtualService
metadata:
  name: bibby
spec:
  hosts:
  - bibby-service
  http:
  - match:
    - headers:
        user-type:
          exact: "beta-tester"  # Beta testers get v1.1
    route:
    - destination:
        host: bibby-service
        subset: v2
  - route:
    - destination:
        host: bibby-service
        subset: v1
      weight: 90  # 90% of traffic
    - destination:
        host: bibby-service
        subset: v2
      weight: 10  # 10% of traffic (canary)
```

**3. Progressive rollout:**
```bash
# Start: 10% canary
kubectl apply -f canary-10-percent.yaml

# Monitor metrics (error rate, latency)
# If good after 1 hour:

# Increase to 50%
kubectl apply -f canary-50-percent.yaml

# If still good after another hour:

# Increase to 100%
kubectl apply -f canary-100-percent.yaml
```

**Automated canary with Flagger:**

```yaml
apiVersion: flagger.app/v1beta1
kind: Canary
metadata:
  name: bibby
spec:
  targetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: bibby
  service:
    port: 8080
  analysis:
    interval: 1m
    threshold: 5  # Rollback after 5 failed checks
    maxWeight: 50
    stepWeight: 10  # Increase traffic by 10% each step
    metrics:
    - name: request-success-rate
      thresholdRange:
        min: 99  # Rollback if success rate < 99%
    - name: request-duration
      thresholdRange:
        max: 500  # Rollback if p99 latency > 500ms
```

**Flagger automatically:**
- Deploys new version
- Increases traffic gradually (10%, 20%, 30%, ...)
- Monitors metrics
- Rolls back automatically if metrics degrade

---

## Trunk-Based Development

**Problem:** Long-lived feature branches → merge conflicts, integration issues.

**Solution:** Everyone commits to `main` daily.

**How?**
- **Feature flags:** Hide incomplete features
- **Branch by abstraction:** Refactor without breaking
- **Small commits:** Each commit is deployable

### Feature Flags Example

```java
@Service
public class BookService {

    @Value("${features.new-search-algorithm.enabled}")
    private boolean newSearchEnabled;

    public List<Book> searchBooks(String query) {
        if (newSearchEnabled) {
            return newSearchAlgorithm(query);  // New code
        } else {
            return legacySearch(query);  // Old code
        }
    }
}
```

**Deploy to production with flag OFF:**
```yaml
# ConfigMap
features:
  new-search-algorithm:
    enabled: false
```

**Enable for 10% of users:**
```java
if (newSearchEnabled && userId % 10 == 0) {
    return newSearchAlgorithm(query);
}
```

**Enable for everyone:**
```yaml
features:
  new-search-algorithm:
    enabled: true
```

**Remove old code in next release.**

---

## Real-World War Story: Knight Capital (2012)

**Incident:** Software deployment error caused $440 million loss in 45 minutes.

**Root cause:** Deployment script failed to update all 8 servers. 7 ran new code, 1 ran old code with a dormant bug flag.

**Lessons:**
1. **Idempotent deployments:** Script should work even if run multiple times
2. **Smoke tests:** Verify deployment before sending traffic
3. **Automated rollback:** Detect issues, roll back automatically
4. **Canary releases:** Deploy to 1 server first, not all 8 simultaneously

**How Bibby's pipeline prevents this:**
- Kubernetes rollout waits for readiness probes (smoke tests)
- Deployment is atomic (all pods update or none)
- Rollback is one command: `kubectl rollout undo`

---

## Action Items

**For Bibby:**

1. **Create GitHub Actions workflow** (30 min)
   - Copy `.github/workflows/ci-cd.yml` from this section
   - Add secrets to GitHub repo
   - Push to trigger pipeline

2. **Add unit tests** (1 hour)
   - Test `BookService.createNewBook()`
   - Run locally: `mvn test`
   - Verify in GitHub Actions

3. **Add integration test** (1 hour)
   - Add Testcontainers dependency
   - Test database persistence
   - Run: `mvn verify`

4. **Set up ArgoCD** (optional, 2 hours)
   - Install ArgoCD on Minikube
   - Create Application manifest
   - Test GitOps workflow

**For your project:**

1. **Automate builds** - Every commit should trigger CI
2. **Require tests** - Block PRs if tests fail
3. **Scan images** - Integrate Trivy or Snyk
4. **Deploy to staging** - Automatically after merge to main
5. **Manual approval for production** - GitHub environments

---

## Further Reading

- **Continuous Delivery:** Jez Humble, David Farley (the bible)
- **Accelerate:** Nicole Forsgren (DevOps research)
- **GitHub Actions Docs:** https://docs.github.com/en/actions
- **ArgoCD:** https://argo-cd.readthedocs.io/
- **Flagger:** https://flagger.app/

---

## Next Section Preview

**Section 15: Observability & Monitoring** will teach you:
- The three pillars: Logs, Metrics, Traces
- Prometheus for metrics collection
- Grafana for visualization
- ELK/EFK stack for log aggregation
- Distributed tracing with Jaeger/Zipkin
- Alerting strategies (on-call, escalation)
- SLOs and error budgets in practice
- Observability-driven development

We'll instrument Bibby with Prometheus metrics, ship logs to Elasticsearch, and trace requests across services.

Ready? Let's make systems observable.

---

**Word count:** ~4,200 words
