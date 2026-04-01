package com.loadbalancing.load.service;

import com.loadbalancing.load.config.LoadBalancerProperties;
import com.loadbalancing.load.model.BackendServer;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

@Service
public class ServerRegistry {

    private final List<BackendServer> servers;

    public ServerRegistry(LoadBalancerProperties properties) {
        List<String> backends = properties.getBackends();
        if (backends == null || backends.isEmpty()) {
            throw new IllegalStateException("No backends configured in loadbalancer.backends");
        }
        this.servers = backends.stream()
                .map(BackendServer::new)
                .toList();
    }

    public List<BackendServer> all() {
        return Collections.unmodifiableList(servers);
    }

    public List<BackendServer> live() {
        return servers.stream()
                .filter(BackendServer::isAlive)
                .toList();
    }
}
