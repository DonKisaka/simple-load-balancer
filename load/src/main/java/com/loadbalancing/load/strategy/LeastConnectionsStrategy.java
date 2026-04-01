package com.loadbalancing.load.strategy;

import com.loadbalancing.load.model.BackendServer;
import java.util.Comparator;
import java.util.List;

public class LeastConnectionsStrategy implements LoadBalancingStrategy {

    @Override
    public BackendServer next(List<BackendServer> servers) {
        if (servers == null || servers.isEmpty()) {
            return null;
        }
        // Note: selection and increment are not atomic — two concurrent calls may both pick
        // the same server under high concurrency. This eventual-consistency behaviour is
        // acceptable for this load balancer; add a lock here if strict distribution is needed.
        return servers.stream()
                .min(Comparator.comparingInt(BackendServer::getActiveConnectionCount))
                .orElse(null);
    }
}
