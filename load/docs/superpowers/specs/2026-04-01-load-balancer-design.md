# Load Balancer — Design Spec
**Date:** 2026-04-01

## Context

A small, educational reverse-proxy load balancer built on Spring Boot 4.0.5 / WebFlux / Java 25. The goal is to learn systems thinking, networking, availability, and resilience through hands-on implementation.

## Stack

- Spring Boot 4.0.5, Java 25, Spring WebFlux (reactive, non-blocking)
- `WebClient` for forwarding requests and health pings
- `@Scheduled` for dead-server recovery polling
- Static backend URLs configured in `application.yml`

## Components

| Class | Package | Responsibility |
|---|---|---|
| `BackendServer` | `model` | Holds url, `volatile boolean isAlive`, `AtomicInteger activeConnections` |
| `LoadBalancerProperties` | `config` | Binds `loadbalancer.backends` + `loadbalancer.algorithm` from yml |
| `ServerRegistry` | `service` | Maps URL strings → `BackendServer` objects; exposes `all()` and `live()` |
| `LoadBalancingStrategy` | `strategy` | Interface: `BackendServer next(List<BackendServer> servers)` |
| `RoundRobinStrategy` | `strategy` | Atomic counter cycling through live servers |
| `LeastConnectionsStrategy` | `strategy` | Picks server with lowest `activeConnections` |
| `StrategyFactory` | `strategy` | `@Bean` selecting strategy from config |
| `LoadBalancerFilter` | `filter` | `WebFilter`: picks backend, forwards via `WebClient`, handles failure |
| `WebClientConfig` | `config` | `@Bean WebClient` |
| `RecoveryScheduler` | `scheduler` | Every 30s pings dead servers; flips `isAlive = true` if reachable |

## Data Flow

```
Request → LoadBalancerFilter
    → serverRegistry.live() → strategy.next()
    → WebClient forwards (method + path + query + filtered headers + body)
        ├── success → copy status + headers + stream body back to client
        └── failure → mark server isAlive=false → 503 Service Unavailable

RecoveryScheduler (every 30s)
    → ping each dead server at GET /
    → if reachable → isAlive = true
```

## Configuration

```yaml
loadbalancer:
  algorithm: round-robin   # or: least-connections
  backends:
    - http://localhost:8081
    - http://localhost:8082
```

## Verification

1. Run `./mvnw spring-boot:run` — app starts on port 8080
2. Start backends: `python -m http.server 8081` and `python -m http.server 8082`
3. `curl http://localhost:8080/` repeatedly — observe requests alternating in logs
4. Switch to `least-connections`, restart, repeat
5. Kill one backend → 503 logged, traffic routes to remaining server
6. Restart dead backend → recovery scheduler logs it alive within 30s
7. Kill both → `503 No available backends`
