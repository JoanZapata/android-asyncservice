package com.joanzap.minim.api;

public abstract class BaseEvent {

    private boolean cached;

    public boolean isCached() {
        return cached;
    }

    public void setCached(boolean cached) {
        this.cached = cached;
    }
}
