package com.loadbalancing.load.strategy;

import com.loadbalancing.load.model.BackendServer;
import java.util.List;

public interface LoadBalancingStrategy {
    BackendServer next(List<BackendServer> servers);
}
