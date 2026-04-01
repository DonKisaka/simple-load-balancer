package com.loadbalancing.load.filter;

import com.loadbalancing.load.model.BackendServer;
import com.loadbalancing.load.service.ServerRegistry;
import com.loadbalancing.load.strategy.LoadBalancingStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

// Runs before all other filters to act as a full reverse proxy.
@Component
@Order(-1)
public class LoadBalancerFilter implements WebFilter {

    private static final Logger log = LoggerFactory.getLogger(LoadBalancerFilter.class);

    // Hop-by-hop headers must not be forwarded between proxy and backend/client.
    private static final Set<String> HOP_BY_HOP_HEADERS = Set.of(
            HttpHeaders.CONNECTION, "keep-alive", HttpHeaders.TRANSFER_ENCODING,
            HttpHeaders.TE, "trailers", "proxy-authorization", "proxy-authenticate",
            HttpHeaders.UPGRADE, HttpHeaders.CONTENT_LENGTH
    );

    private final ServerRegistry serverRegistry;
    private final LoadBalancingStrategy strategy;
    private final WebClient webClient;

    public LoadBalancerFilter(ServerRegistry serverRegistry,
                               LoadBalancingStrategy strategy,
                               WebClient webClient) {
        this.serverRegistry = serverRegistry;
        this.strategy = strategy;
        this.webClient = webClient;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        List<BackendServer> liveServers = serverRegistry.live();

        if (liveServers.isEmpty()) {
            return write503(exchange, "No available backends");
        }

        BackendServer backend = strategy.next(liveServers);

        URI originalUri = exchange.getRequest().getURI();
        String rawQuery = originalUri.getRawQuery();
        // Strip trailing slash from backend URL to avoid double-slash paths.
        String baseUrl = backend.getUrl().endsWith("/")
                ? backend.getUrl().substring(0, backend.getUrl().length() - 1)
                : backend.getUrl();
        URI targetUri = URI.create(
                baseUrl
                + originalUri.getRawPath()
                + (rawQuery != null ? "?" + rawQuery : "")
        );

        return webClient
                .method(exchange.getRequest().getMethod())
                .uri(targetUri)
                .headers(h -> {
                    exchange.getRequest().getHeaders().forEach((name, values) -> {
                        if (!HOP_BY_HOP_HEADERS.contains(name.toLowerCase())) {
                            h.addAll(name, values);
                        }
                    });
                    h.remove(HttpHeaders.HOST);
                })
                .body(BodyInserters.fromDataBuffers(exchange.getRequest().getBody()))
                .exchangeToMono(clientResponse -> {
                    exchange.getResponse().setStatusCode(clientResponse.statusCode());
                    // Copy response headers, excluding hop-by-hop headers.
                    clientResponse.headers().asHttpHeaders().forEach((name, values) -> {
                        if (!HOP_BY_HOP_HEADERS.contains(name.toLowerCase())) {
                            exchange.getResponse().getHeaders().addAll(name, values);
                        }
                    });
                    return exchange.getResponse().writeWith(
                            clientResponse.bodyToFlux(DataBuffer.class)
                    );
                })
                .doFirst(backend::incrementConnections)
                .doFinally(signal -> backend.decrementConnections())
                .onErrorResume(t -> {
                    backend.setAlive(false);
                    log.warn("Backend {} marked as unavailable", backend.getUrl(), t);
                    if (exchange.getResponse().isCommitted()) {
                        return Mono.empty();
                    }
                    return write503(exchange, "Backend unavailable");
                });
    }

    private Mono<Void> write503(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        byte[] bytes = message.getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
