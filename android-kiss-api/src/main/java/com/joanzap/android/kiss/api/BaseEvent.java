package com.joanzap.android.kiss.api;

import java.lang.ref.WeakReference;

public abstract class BaseEvent {

    private boolean cached;

    private WeakReference emitter;

    public boolean isCached() {
        return cached;
    }

    public void setCached(boolean cached) {
        this.cached = cached;
    }

    Object getEmitter() {
        return emitter.get();
    }

    void setEmitter(Object emitter) {
        this.emitter = new WeakReference(emitter);
    }
}
