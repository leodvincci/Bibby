# Section 13: Orchestration with Kubernetes

**Part IV: Infrastructure & Deployment**

---

## Docker Compose Doesn't Scale to Production

You've containerized Bibby. You can run it locally with Docker Compose. **Congratulations. You're 10% done.**

Here's what Docker Compose **can't** do:

1. **Auto-scaling:** Traffic spikes. You need 10 Bibby instances. How?
2. **Self-healing:** Container crashes. Who restarts it?
3. **Load balancing:** 10 instances running. How does traffic distribute?
4. **Rolling updates:** Deploy version 1.1 without downtime. How?
5. **Multi-host:** Bibby needs 20GB RAM. Your laptop has 16GB. How do you run across multiple machines?
6. **Service discovery:** Catalog Service needs to find Library Service. IP addresses change. How?
7. **Secrets management:** Database passwords. Can't hardcode in docker-compose.yml. How do you inject securely?

**The answer:** Container orchestration. Specifically, **Kubernetes**.

---

## What Is Kubernetes?

**Kubernetes (k8s):** Open-source container orchestration platform.

**Created by:** Google (2014), based on their internal Borg system (managing 2+ billion containers/week).

**Mental model:** Kubernetes is the **operating system for your data center**.

| Traditional OS | Kubernetes (Cluster OS) |
|----------------|------------------------|
| Manages processes | Manages containers |
| Schedules tasks on CPU cores | Schedules pods on nodes |
| Restarts crashed processes | Restarts crashed pods |
| Load balances across cores | Load balances across pods |
| Provides file system | Provides persistent volumes |
| Exposes network interfaces | Exposes services |

**Key insight:** You declare **desired state** ("I want 3 Bibby pods running"). Kubernetes ensures **actual state** matches desired state.

**Declarative vs Imperative:**

**Imperative (Docker):**
```bash
docker run bibby:1.0  # Start container
docker run bibby:1.0  # Start another
docker run bibby:1.0  # Start another
# If one crashes, you manually restart it
```

**Declarative (Kubernetes):**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: bibby
spec:
  replicas: 3  # ← Desired state: 3 pods
```

**Kubernetes control loop:**
```
while true:
  desired_state = read_from_config()
  actual_state = check_cluster()

  if actual_state != desired_state:
    reconcile()  # Start pods, kill pods, reschedule, etc.

  sleep(5 seconds)
```

**Result:** Pod crashes? Kubernetes auto-restarts. Node dies? Kubernetes reschedules pods elsewhere.

---

## Kubernetes Architecture (30-Second Overview)

```
┌─────────────────────────────────────────────────────────┐
│                  Control Plane (Master)                 │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │ API Server   │  │  Scheduler   │  │  Controller  │  │
│  │ (kubectl →)  │  │ (assigns     │  │  Manager     │  │
│  │              │  │  pods to     │  │ (reconciles  │  │
│  │              │  │  nodes)      │  │  state)      │  │
│  └──────────────┘  └──────────────┘  └──────────────┘  │
│                                                         │
│  ┌──────────────┐                                       │
│  │   etcd       │  ← Distributed key-value store       │
│  │ (stores all  │     (cluster brain)                  │
│  │  config)     │                                       │
│  └──────────────┘                                       │
└─────────────────────────────────────────────────────────┘
         │                    │                    │
    ┌────┴────┐          ┌────┴────┐         ┌────┴────┐
    │ Node 1  │          │ Node 2  │         │ Node 3  │
    │ ┌─────┐ │          │ ┌─────┐ │         │ ┌─────┐ │
    │ │ Pod │ │          │ │ Pod │ │         │ │ Pod │ │
    │ │ Pod │ │          │ │ Pod │ │         │ │ Pod │ │
    │ └─────┘ │          │ └─────┘ │         │ └─────┘ │
    │ kubelet │          │ kubelet │         │ kubelet │
    └─────────┘          └─────────┘         └─────────┘
```

**For local dev:** Use **Minikube** (single-node cluster on your laptop).

---

## Core Concepts

### 1. Pod

**Definition:** Smallest deployable unit. Contains 1+ containers.

**Analogy:** A process in an OS.

**Bibby example:**
```yaml
apiVersion: v1
kind: Pod
metadata:
  name: bibby-pod
spec:
  containers:
  - name: bibby
    image: bibby:1.0
    ports:
    - containerPort: 8080
    env:
    - name: SPRING_DATASOURCE_URL
      value: "jdbc:postgresql://postgres:5432/amigos"
```

**Key point:** Pods are **ephemeral**. They die. They get recreated. **Never rely on a specific pod existing.**

### 2. Deployment

**Definition:** Manages a set of identical pods. Provides rollouts, rollbacks, scaling.

**Analogy:** A systemd service that restarts crashed processes.

**Bibby Deployment:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: bibby-deployment
  labels:
    app: bibby
spec:
  replicas: 3  # Run 3 pods
  selector:
    matchLabels:
      app: bibby
  template:  # Pod template
    metadata:
      labels:
        app: bibby
    spec:
      containers:
      - name: bibby
        image: bibby:1.0
        ports:
        - containerPort: 8080
        resources:
          requests:
            memory: "512Mi"
            cpu: "500m"  # 0.5 CPU
          limits:
            memory: "1Gi"
            cpu: "1000m"  # 1 CPU
        env:
        - name: SPRING_DATASOURCE_URL
          value: "jdbc:postgresql://postgres-service:5432/amigos"
        livenessProbe:
          httpGet:
            path: /actuator/health/liveness
            port: 8080
          initialDelaySeconds: 30
          periodSeconds: 10
        readinessProbe:
          httpGet:
            path: /actuator/health/readiness
            port: 8080
          initialDelaySeconds: 10
          periodSeconds: 5
```

**What this does:**
- Creates 3 pods running Bibby
- Each pod limited to 1GB RAM, 1 CPU
- Liveness probe: Restart pod if health check fails
- Readiness probe: Don't send traffic until pod is ready

**Deploy it:**
```bash
kubectl apply -f bibby-deployment.yaml

# Check status
kubectl get deployments
kubectl get pods

# Output:
NAME                              READY   STATUS    RESTARTS   AGE
bibby-deployment-5d9f7c8b4-abc12   1/1     Running   0          30s
bibby-deployment-5d9f7c8b4-def34   1/1     Running   0          30s
bibby-deployment-5d9f7c8b4-ghi56   1/1     Running   0          30s
```

### 3. Service

**Problem:** Pods have dynamic IPs. Pod dies, new pod gets new IP. How do clients find pods?

**Solution:** Service (stable IP + DNS name that load balances across pods).

**Bibby Service:**
```yaml
apiVersion: v1
kind: Service
metadata:
  name: bibby-service
spec:
  selector:
    app: bibby  # Route traffic to pods with label app=bibby
  ports:
  - protocol: TCP
    port: 80        # Service listens on port 80
    targetPort: 8080  # Forwards to pod's port 8080
  type: LoadBalancer  # Expose to external traffic
```

**How it works:**
```
External request → LoadBalancer (Cloud provider) → Service (bibby-service:80)
  ↓
Service load balances across pods:
  → bibby-pod-1 (10.0.1.5:8080)
  → bibby-pod-2 (10.0.1.6:8080)
  → bibby-pod-3 (10.0.1.7:8080)
```

**DNS magic:** Inside the cluster, any pod can reach Bibby via `http://bibby-service`.

**Service types:**
- **ClusterIP:** Internal only (default). Service reachable only within cluster.
- **NodePort:** Exposes service on each node's IP at a static port (30000-32767).
- **LoadBalancer:** Cloud provider creates external load balancer (AWS ELB, GCP LB).
- **ExternalName:** Maps service to external DNS (e.g., legacy database).

### 4. ConfigMap

**Problem:** Hardcoding config in container images is bad (can't change without rebuild).

**Solution:** ConfigMap (key-value config, injected at runtime).

**Bibby ConfigMap:**
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: bibby-config
data:
  application.properties: |
    spring.application.name=Bibby
    spring.shell.interactive.enabled=true
    server.error.include-message=always
    spring.output.ansi.enabled=ALWAYS
```

**Use in Deployment:**
```yaml
spec:
  containers:
  - name: bibby
    image: bibby:1.0
    volumeMounts:
    - name: config
      mountPath: /app/config
      readOnly: true
  volumes:
  - name: config
    configMap:
      name: bibby-config
```

**Result:** `/app/config/application.properties` contains the config from ConfigMap.

### 5. Secret

**Problem:** ConfigMaps are **not encrypted**. Don't store passwords in ConfigMaps.

**Solution:** Secret (base64-encoded, optionally encrypted at rest).

**Bibby Secret:**
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: bibby-db-secret
type: Opaque
data:
  username: YW1pZ29zY29kZQ==  # base64("amigoscode")
  password: cGFzc3dvcmQ=      # base64("password")
```

**Use in Deployment:**
```yaml
spec:
  containers:
  - name: bibby
    image: bibby:1.0
    env:
    - name: SPRING_DATASOURCE_USERNAME
      valueFrom:
        secretKeyRef:
          name: bibby-db-secret
          key: username
    - name: SPRING_DATASOURCE_PASSWORD
      valueFrom:
        secretKeyRef:
          name: bibby-db-secret
          key: password
```

**Create secret from command line:**
```bash
kubectl create secret generic bibby-db-secret \
  --from-literal=username=amigoscode \
  --from-literal=password=password
```

**Security note:** Secrets are base64-encoded (NOT encrypted) by default. For production, enable **encryption at rest** in etcd.

### 6. PersistentVolume & PersistentVolumeClaim

**Problem:** Containers are ephemeral. Database pod dies, data is lost.

**Solution:** PersistentVolume (storage that outlives pods).

**PostgreSQL StatefulSet + PVC:**
```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: postgres-pvc
spec:
  accessModes:
  - ReadWriteOnce  # Single node can mount read-write
  resources:
    requests:
      storage: 10Gi
---
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: postgres
spec:
  serviceName: postgres-service
  replicas: 1
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
    spec:
      containers:
      - name: postgres
        image: postgres:15-alpine
        ports:
        - containerPort: 5432
        env:
        - name: POSTGRES_DB
          value: amigos
        - name: POSTGRES_USER
          valueFrom:
            secretKeyRef:
              name: bibby-db-secret
              key: username
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: bibby-db-secret
              key: password
        volumeMounts:
        - name: postgres-storage
          mountPath: /var/lib/postgresql/data
      volumes:
      - name: postgres-storage
        persistentVolumeClaim:
          claimName: postgres-pvc
```

**What this does:**
- Requests 10GB of storage
- Mounts volume at `/var/lib/postgresql/data` (PostgreSQL data directory)
- Pod dies? Kubernetes creates new pod, mounts **same volume** → data persists

**StatefulSet vs Deployment:**
- **Deployment:** For stateless apps (Bibby). Pods are interchangeable.
- **StatefulSet:** For stateful apps (databases). Pods have stable identities (postgres-0, postgres-1).

---

## Deploying Bibby to Kubernetes

### Complete Manifests

**1. Namespace (logical isolation):**
```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: bibby
```

**2. Secret:**
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: bibby-db-secret
  namespace: bibby
type: Opaque
stringData:  # Unencoded (kubectl will encode)
  username: amigoscode
  password: password
```

**3. PostgreSQL PVC:**
```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: postgres-pvc
  namespace: bibby
spec:
  accessModes:
  - ReadWriteOnce
  resources:
    requests:
      storage: 5Gi
```

**4. PostgreSQL StatefulSet:**
```yaml
apiVersion: apps/v1
kind: StatefulSet
metadata:
  name: postgres
  namespace: bibby
spec:
  serviceName: postgres-service
  replicas: 1
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
    spec:
      containers:
      - name: postgres
        image: postgres:15-alpine
        ports:
        - containerPort: 5432
        env:
        - name: POSTGRES_DB
          value: amigos
        - name: POSTGRES_USER
          valueFrom:
            secretKeyRef:
              name: bibby-db-secret
              key: username
        - name: POSTGRES_PASSWORD
          valueFrom:
            secretKeyRef:
              name: bibby-db-secret
              key: password
        volumeMounts:
        - name: postgres-storage
          mountPath: /var/lib/postgresql/data
          subPath: postgres  # Avoid permission issues
      volumes:
      - name: postgres-storage
        persistentVolumeClaim:
          claimName: postgres-pvc
```

**5. PostgreSQL Service:**
```yaml
apiVersion: v1
kind: Service
metadata:
  name: postgres-service
  namespace: bibby
spec:
  selector:
    app: postgres
  ports:
  - protocol: TCP
    port: 5432
    targetPort: 5432
  clusterIP: None  # Headless service (for StatefulSet)
```

**6. Bibby Deployment:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: bibby
  namespace: bibby
spec:
  replicas: 2
  selector:
    matchLabels:
      app: bibby
  template:
    metadata:
      labels:
        app: bibby
    spec:
      containers:
      - name: bibby
        image: bibby:1.0
        imagePullPolicy: Never  # Use local image (Minikube)
        ports:
        - containerPort: 8080
        env:
        - name: SPRING_DATASOURCE_URL
          value: "jdbc:postgresql://postgres-service.bibby.svc.cluster.local:5432/amigos"
        - name: SPRING_DATASOURCE_USERNAME
          valueFrom:
            secretKeyRef:
              name: bibby-db-secret
              key: username
        - name: SPRING_DATASOURCE_PASSWORD
          valueFrom:
            secretKeyRef:
              name: bibby-db-secret
              key: password
        resources:
          requests:
            memory: "512Mi"
            cpu: "250m"
          limits:
            memory: "1Gi"
            cpu: "500m"
```

**7. Bibby Service:**
```yaml
apiVersion: v1
kind: Service
metadata:
  name: bibby-service
  namespace: bibby
spec:
  selector:
    app: bibby
  ports:
  - protocol: TCP
    port: 8080
    targetPort: 8080
  type: NodePort  # Expose via node IP
```

### Deploy Everything

```bash
# Start Minikube
minikube start

# Build Docker image in Minikube's Docker daemon
eval $(minikube docker-env)
docker build -t bibby:1.0 .

# Apply all manifests
kubectl apply -f k8s/

# Watch pods come up
kubectl get pods -n bibby --watch

# Output:
NAME                     READY   STATUS    RESTARTS   AGE
postgres-0               1/1     Running   0          45s
bibby-5d9f7c8b4-abc12    1/1     Running   0          30s
bibby-5d9f7c8b4-def34    1/1     Running   0          30s

# Access Bibby (get NodePort)
kubectl get svc -n bibby bibby-service

# Output:
NAME            TYPE       CLUSTER-IP     EXTERNAL-IP   PORT(S)          AGE
bibby-service   NodePort   10.96.200.10   <none>        8080:31234/TCP   1m

# Access via Minikube
minikube service bibby-service -n bibby
```

---

## Scaling

### Manual Scaling

```bash
# Scale to 5 replicas
kubectl scale deployment bibby -n bibby --replicas=5

# Verify
kubectl get pods -n bibby

# Output:
NAME                     READY   STATUS    RESTARTS   AGE
bibby-5d9f7c8b4-abc12    1/1     Running   0          5m
bibby-5d9f7c8b4-def34    1/1     Running   0          5m
bibby-5d9f7c8b4-ghi56    1/1     Running   0          10s
bibby-5d9f7c8b4-jkl78    1/1     Running   0          10s
bibby-5d9f7c8b4-mno90    1/1     Running   0          10s
```

### Horizontal Pod Autoscaler (HPA)

**Auto-scale based on CPU/memory usage.**

```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: bibby-hpa
  namespace: bibby
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: bibby
  minReplicas: 2
  maxReplicas: 10
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70  # Scale up if CPU > 70%
```

**How it works:**
```
CPU usage at 80% → HPA scales from 2 → 4 replicas
CPU usage drops to 40% → HPA scales from 4 → 2 replicas
```

**Enable metrics server (Minikube):**
```bash
minikube addons enable metrics-server

# Apply HPA
kubectl apply -f bibby-hpa.yaml

# Watch autoscaling
kubectl get hpa -n bibby --watch
```

---

## Rolling Updates & Rollbacks

### Rolling Update

**Update Bibby from v1.0 to v1.1:**

```bash
# Update deployment image
kubectl set image deployment/bibby bibby=bibby:1.1 -n bibby

# Or edit deployment directly
kubectl edit deployment bibby -n bibby
# Change: image: bibby:1.0 → image: bibby:1.1

# Watch rollout
kubectl rollout status deployment/bibby -n bibby

# Output:
Waiting for deployment "bibby" rollout to finish: 1 out of 2 new replicas updated...
Waiting for deployment "bibby" rollout to finish: 1 old replicas pending termination...
deployment "bibby" successfully rolled out
```

**What happens:**
```
Time 0s:  2 pods running v1.0
Time 10s: 1 pod running v1.1, 2 pods running v1.0 (surge)
Time 20s: 2 pods running v1.1, 1 pod running v1.0
Time 30s: 2 pods running v1.1, 0 pods running v1.0
```

**Strategy configuration:**
```yaml
spec:
  strategy:
    type: RollingUpdate
    rollingUpdate:
      maxSurge: 1        # Allow 1 extra pod during rollout
      maxUnavailable: 0  # Never have fewer than desired replicas
```

### Rollback

**v1.1 has a bug. Rollback immediately:**

```bash
# Rollback to previous version
kubectl rollout undo deployment/bibby -n bibby

# Verify
kubectl rollout status deployment/bibby -n bibby

# Rollback to specific revision
kubectl rollout undo deployment/bibby -n bibby --to-revision=2

# View rollout history
kubectl rollout history deployment/bibby -n bibby

# Output:
REVISION  CHANGE-CAUSE
1         <none>
2         Image updated to bibby:1.1
3         Image updated to bibby:1.0
```

---

## Service Discovery

**How does Catalog Service find Library Service?**

**DNS-based (automatic in Kubernetes):**

```java
// In Catalog Service
@FeignClient(name = "library-service", url = "http://library-service.bibby.svc.cluster.local")
public interface LibraryServiceClient {
    @GetMapping("/api/v1/locations/{shelfId}")
    LocationInfo getLocation(@PathVariable Long shelfId);
}
```

**DNS name format:** `<service-name>.<namespace>.svc.cluster.local`

**Examples:**
- Same namespace: `http://library-service` (short form)
- Different namespace: `http://library-service.bibby.svc.cluster.local`
- External service: `http://my-api.default.svc.cluster.local`

**Behind the scenes:** Kubernetes DNS (CoreDNS) resolves service names to ClusterIPs.

---

## Health Checks Revisited

**Liveness Probe:** Is the app alive? (If not, restart it.)
**Readiness Probe:** Is the app ready for traffic? (If not, remove from load balancer.)

**Why both?**

**Scenario:** Bibby starts up. Takes 30 seconds to connect to database.

**Without readiness probe:**
```
t=0s:  Pod starts
t=0s:  Service sends traffic → 500 errors (DB not connected yet)
t=30s: Bibby ready, but users already saw errors
```

**With readiness probe:**
```
t=0s:  Pod starts
t=0s:  Readiness probe fails → Not added to service endpoints
t=30s: Readiness probe succeeds → Traffic starts flowing
```

**Bibby probes (from earlier):**
```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 30  # Wait 30s before first check
  periodSeconds: 10        # Check every 10s
  failureThreshold: 3      # Restart after 3 failures

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 10  # Check readiness sooner
  periodSeconds: 5
  failureThreshold: 2      # Remove from LB after 2 failures
```

---

## Real-World War Story: Pokémon GO Launch (2016)

**Problem:** Game launched. 50x expected traffic. Servers crashed.

**Root cause:** Fixed number of servers. No auto-scaling.

**Solution:** Migrated to Google Kubernetes Engine (GKE).
- Horizontal Pod Autoscaler scaled from 500 → 5000 pods automatically
- Self-healing restarted crashed pods
- Rolling updates deployed fixes without downtime

**Result:** Handled 500M+ requests/second during peak.

**Lesson:** Kubernetes enables **elastic infrastructure**. Scale up during traffic spikes, scale down to save money.

---

## Anti-Patterns to Avoid

### 1. Running Databases in Kubernetes (Maybe)

**Debate:** Should you run PostgreSQL in Kubernetes?

**Arguments against:**
- Databases need stable storage (complexity with PVs)
- Backups harder to manage
- Cloud-managed databases (AWS RDS) handle this better

**Arguments for:**
- Development/staging environments (full stack in k8s)
- Self-managed infra (no cloud provider)
- Operators (CrunchyData Postgres Operator) simplify management

**Recommendation:** Use managed databases (RDS, Cloud SQL) in production. Use StatefulSets for dev/test.

### 2. Storing Secrets in Git

**❌ NEVER:**
```yaml
# secret.yaml committed to Git
apiVersion: v1
kind: Secret
data:
  password: cGFzc3dvcmQ=  # Anyone can decode this!
```

**✅ USE:**
- **Sealed Secrets:** Encrypt secrets, store encrypted version in Git
- **External Secrets Operator:** Pull secrets from AWS Secrets Manager, HashiCorp Vault
- **Environment-specific:** Different secrets per environment (dev, staging, prod)

### 3. No Resource Limits

**❌ BAD:**
```yaml
containers:
- name: bibby
  image: bibby:1.0
  # No resource limits → Can consume all node memory
```

**Result:** Memory leak in Bibby → Consumes 16GB → Node runs out of memory → All pods on node crash.

**✅ GOOD:**
```yaml
resources:
  requests:
    memory: "512Mi"  # Scheduler reserves this
    cpu: "250m"
  limits:
    memory: "1Gi"    # Pod killed if exceeds
    cpu: "500m"      # Pod throttled if exceeds
```

---

## Action Items

**For Bibby:**

1. **Install Minikube** (15 min)
   ```bash
   # macOS
   brew install minikube
   minikube start
   ```

2. **Create k8s manifests** (30 min)
   - Create `k8s/` directory
   - Copy manifests from this section
   - Deploy: `kubectl apply -f k8s/`

3. **Test scaling** (10 min)
   ```bash
   kubectl scale deployment bibby -n bibby --replicas=5
   kubectl get pods -n bibby
   ```

4. **Test rolling update** (15 min)
   - Modify Bibby code, rebuild image with new tag
   - Update deployment: `kubectl set image ...`
   - Watch rollout: `kubectl rollout status ...`

**For your project:**

1. **Create namespace** - Logical isolation per environment
2. **Set resource limits** - Prevent resource exhaustion
3. **Configure health checks** - Liveness + readiness probes
4. **Enable autoscaling** - HPA based on CPU/memory
5. **Use Secrets** - Never hardcode passwords

---

## Further Reading

- **Kubernetes Official Docs:** https://kubernetes.io/docs/
- **Kubernetes Patterns:** https://www.oreilly.com/library/view/kubernetes-patterns/9781492050285/
- **Production Best Practices:** https://learnk8s.io/production-best-practices
- **Minikube:** https://minikube.sigs.k8s.io/docs/
- **kubectl Cheat Sheet:** https://kubernetes.io/docs/reference/kubectl/cheatsheet/

---

## Next Section Preview

**Section 14: CI/CD for Microservices** will teach you:
- Continuous Integration fundamentals
- Building Docker images in CI pipelines
- Automated testing strategies (unit, integration, e2e)
- Continuous Deployment to Kubernetes
- GitOps with ArgoCD/Flux
- Blue-green deployments
- Canary releases
- Trunk-based development

We'll create a GitHub Actions pipeline that builds Bibby, runs tests, builds Docker image, and deploys to Kubernetes automatically.

Ready? Let's automate everything.

---

**Word count:** ~3,800 words
