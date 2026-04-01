# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Spring Boot 4.0.5 application using Spring WebFlux (reactive) and Spring Boot Actuator. Java 25. Group: `com.loadbalancing`, artifact: `load`.

## Commands

```bash
# Build
./mvnw clean package

# Run
./mvnw spring-boot:run

# Run all tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=LoadApplicationTests

# Run a single test method
./mvnw test -Dtest=LoadApplicationTests#contextLoads
```

## Architecture

Currently a skeleton — only the entry point `LoadApplication.java` exists. The stack is:

- **Spring WebFlux**: reactive, non-blocking HTTP (uses `Mono`/`Flux` instead of blocking `RestTemplate`/`ResponseEntity`). Controllers should be annotated with `@RestController` and return reactive types.
- **Spring Boot Actuator**: health/metrics endpoints available at `/actuator/*` by default.
- **Base package**: `com.loadbalancing.load`