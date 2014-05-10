/**
 * Copyright 2014 Joan Zapata
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.joanzapata.android.kiss.api.internal;

import com.joanzapata.android.kiss.api.BaseEvent;

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
