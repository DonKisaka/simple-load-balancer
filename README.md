# Simple Load Balancer

This project is a simple reverse proxy built with Spring Boot WebFlux. It accepts incoming requests, forwards them to one of several backend servers, and keeps the system available when one backend fails.

## The Problem It Solves

If a single server handles all traffic, the application becomes fragile. One slow or failed server can hurt availability and performance.

This project solves that by:

- distributing requests across multiple backend servers
- supporting different balancing strategies
- detecting failed servers and removing them from rotation
- periodically checking failed servers so they can rejoin once they recover

## How It Works

The load balancer listens for requests and forwards each one to a backend server.

- `round-robin` sends traffic to each healthy server in turn
- `least-connections` sends traffic to the healthy server with the fewest active requests

If a backend cannot be reached, it is marked as unavailable. A scheduled recovery check tries dead servers again every 30 seconds and adds them back once they respond.

## Why These Design Choices

### Spring WebFlux

I used WebFlux because a load balancer is mostly an I/O problem. It spends most of its time receiving requests, forwarding them, and streaming responses back. A reactive stack is a good fit for this kind of network-heavy work.

### Strategy Pattern

The balancing algorithm is separated behind a strategy interface. This keeps the request forwarding logic independent from the server selection logic and makes it easy to switch between `round-robin` and `least-connections`.

### In-Memory Server Registry

The backend list and server health state are kept in memory. This keeps the project simple and easy to understand while still demonstrating the core ideas behind load balancing.

### Passive Failure Detection + Scheduled Recovery

Backends are marked dead when a real request fails. Dead servers are checked again on a schedule. This is a simple way to show resilience without introducing too much infrastructure or complexity.

## Run The Project

### 1. Start two sample backends

In one terminal:

```powershell
mkdir c:\tmp\b1 -Force
"backend-1" | Out-File -Encoding utf8 c:\tmp\b1\index.html
cd c:\tmp\b1
python -m http.server 8081
```

In a second terminal:

```powershell
mkdir c:\tmp\b2 -Force
"backend-2" | Out-File -Encoding utf8 c:\tmp\b2\index.html
cd c:\tmp\b2
python -m http.server 8082
```

### 2. Configure the load balancer

`src/main/resources/application.yml`

```yaml
loadbalancer:
  algorithm: round-robin
  backends:
    - http://localhost:8081
    - http://localhost:8082
```

You can change `algorithm` to `least-connections`.

### 3. Start the Spring Boot app

```powershell
cd C:\Users\Administrator\Downloads\load\load
mvn spring-boot:run
```

### 4. Test it

Send a few requests:

```powershell
curl http://localhost:8080/
curl http://localhost:8080/
curl http://localhost:8080/
```

With `round-robin`, the responses should alternate between `backend-1` and `backend-2`.

To test failure handling, stop one backend and send requests again. The dead backend should be removed from rotation. Restart it and the scheduler should bring it back after the next recovery check.

## What I Would Improve Next

- add a dedicated `/health` endpoint instead of probing `/`
- expose an endpoint that shows active backends and connection counts
- add timeouts, retries, and better error reporting
- add automated tests for routing, failover, and recovery

## Why This Project Is Useful

This project teaches more than just HTTP forwarding. It touches core distributed-systems ideas like availability, failure handling, traffic distribution, and recovery. It is small enough to understand quickly, but realistic enough to show how resilient systems are built.
