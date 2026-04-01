package com.loadbalancing.load.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties("loadbalancer")
public class LoadBalancerProperties {

    private List<String> backends = new java.util.ArrayList<>();
    private String algorithm;

    public List<String> getBackends() {
        return backends;
    }

    public void setBackends(List<String> backends) {
        this.backends = backends;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public void setAlgorithm(String algorithm) {
        this.algorithm = algorithm;
    }
}
