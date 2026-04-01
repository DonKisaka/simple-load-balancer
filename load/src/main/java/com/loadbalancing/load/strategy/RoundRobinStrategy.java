package com.loadbalancing.load.strategy;

import com.loadbalancing.load.model.BackendServer;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinStrategy implements LoadBalancingStrategy {

    private final AtomicInteger counter = new AtomicInteger(0);

    @Override
    public BackendServer next(List<BackendServer> servers) {
        if (servers == null || servers.isEmpty()) {
            return null;
        }
        int index = (counter.getAndIncrement() & Integer.MAX_VALUE) % servers.size();
        return servers.get(index);
    }
}
