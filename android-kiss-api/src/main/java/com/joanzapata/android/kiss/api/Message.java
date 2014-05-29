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
package com.joanzapata.android.kiss.api;

import java.io.Serializable;
import java.lang.ref.WeakReference;

public final class Message<T> implements Serializable {

    private boolean cached;

    private WeakReference<Object> emitter;

    private final T payload;

    /**
     * Contains a description of the query,
     * using class name, method name, and params,
     * that generated this event as a response.
     */
    private String query;

    public Message(T payload) {
        this.payload = payload;
    }

    public boolean isCached() {
        return cached;
    }

    public Message cached() {
        cached = true;
        return this;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public String getQuery() {
        return query;
    }

    public Object getEmitter() {
        return emitter.get();
    }

    public void setEmitter(Object emitter) {
        this.emitter = new WeakReference<Object>(emitter);
    }

    public T getPayload() {
        return payload;
    }
}
