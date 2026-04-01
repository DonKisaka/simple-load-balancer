package com.loadbalancing.load.model;

import java.util.concurrent.atomic.AtomicInteger;

public class BackendServer {

    private final String url;
    private volatile boolean isAlive = true;
    private final AtomicInteger activeConnections = new AtomicInteger(0);

    public BackendServer(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public boolean isAlive() {
        return isAlive;
    }

    public void setAlive(boolean alive) {
        this.isAlive = alive;
    }

    public void incrementConnections() {
        activeConnections.incrementAndGet();
    }

    public void decrementConnections() {
        activeConnections.decrementAndGet();
    }

    public int getActiveConnectionCount() {
        return activeConnections.get();
    }
}
