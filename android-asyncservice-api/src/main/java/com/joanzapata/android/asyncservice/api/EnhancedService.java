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
package com.joanzapata.android.asyncservice.api;

import java.io.Serializable;
import java.util.List;

public interface EnhancedService {

    /**
     * Send a message to the current caller.
     * @param message The message you want to send.
     */
    void send(Object message);

    /**
     * Cache an object at the given key. (override previous value if any)
     * @param key    The key at which you want to store the object.
     * @param object A serializable object to store.
     */
    void cache(String key, Serializable object);

    /** Same as #cache for lists */
    void cacheList(String key, List<? extends Serializable> object);

    /**
     * Retrieve the cache value at the given key.
     * @param key        The key at which you previously stored the object.
     * @param returnType The expected return type.
     * @return The last cached value at the given key, or null if
     * nothing was stored here before, or null if the return type
     * has changed since the last time the object was store.
     */
    <T extends Serializable> T getCached(String key, Class<T> returnType);

    /** Same as #getCached but for a list value. */
    <T extends Serializable> List<T> getCachedList(String key, Class<T> returnType);

    /** Remove the value at the given key in the cache */
    void clearCache(String key);

    /** Remove all values in the cache */
    void clearCache();
}
