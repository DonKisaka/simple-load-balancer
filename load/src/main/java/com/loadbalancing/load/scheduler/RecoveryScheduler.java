package com.loadbalancing.load.scheduler;

import com.loadbalancing.load.model.BackendServer;
import com.loadbalancing.load.service.ServerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class RecoveryScheduler {

    private static final Logger log = LoggerFactory.getLogger(RecoveryScheduler.class);

    private final ServerRegistry serverRegistry;
    private final WebClient webClient;

    public RecoveryScheduler(ServerRegistry serverRegistry, WebClient webClient) {
        this.serverRegistry = serverRegistry;
        this.webClient = webClient;
    }

    // Every 30 seconds, ping dead servers and bring them back if reachable.
    @Scheduled(fixedDelay = 30_000)
    public void recoverDeadServers() {
        serverRegistry.all().stream()
                .filter(server -> !server.isAlive())
                .forEach(this::tryRecover);
    }

    private void tryRecover(BackendServer server) {
        webClient.get()
                .uri(server.getUrl() + "/")
                .retrieve()
                .toBodilessEntity()
                .subscribe(
                        response -> {
                            server.setAlive(true);
                            log.info("Backend {} is back online", server.getUrl());
                        },
                        error -> log.debug("Backend {} still unreachable: {}", server.getUrl(), error.getMessage())
                );
    }
}
