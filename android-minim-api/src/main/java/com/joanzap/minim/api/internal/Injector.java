package com.joanzap.minim.api.internal;

import com.joanzap.minim.api.BaseEvent;

import java.lang.ref.WeakReference;

public abstract class Injector<T> {

    protected WeakReference<T> target;

    void setTarget(T target) {
        this.target = new WeakReference<T>(target);
        inject(target);
    }

    /**
     * Dispatch the event to the target.
     * @return true if object is still valid (target is alive)
     * false otherwise. In case of false, this object should
     * not be called anymore.
     */
    boolean dispatch(BaseEvent event) {
        T targetObject = target.get();
        if (targetObject == null) return false;
        dispatch(targetObject, event);
        return true;
    }

    protected abstract void inject(T injectable);

    protected abstract void dispatch(T target, BaseEvent event);
}
