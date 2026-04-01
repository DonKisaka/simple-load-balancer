package com.loadbalancing.load.strategy;

import com.loadbalancing.load.config.LoadBalancerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class StrategyFactory {

    @Bean
    public LoadBalancingStrategy loadBalancingStrategy(LoadBalancerProperties properties) {
        String algorithm = properties.getAlgorithm();
        if (algorithm == null || algorithm.isBlank()) {
            throw new IllegalArgumentException("loadbalancer.algorithm must be configured");
        }
        algorithm = algorithm.toLowerCase(java.util.Locale.ROOT).trim();
        return switch (algorithm) {
            case "round-robin" -> new RoundRobinStrategy();
            case "least-connections" -> new LeastConnectionsStrategy();
            default -> throw new IllegalArgumentException("Unknown algorithm: " + algorithm);
        };
    }
}
