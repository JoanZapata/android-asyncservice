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
package com.joanzap.android.kiss.api.internal;

import com.joanzap.android.kiss.api.BaseCache;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class DefaultCache implements BaseCache {

    private Map<String, Object> map = new HashMap<String, Object>();

    @Override
    public void store(String key, Serializable object) {
        map.put(key, object);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Serializable> T get(String key, Class<T> expectedClass) {
        return (T) map.get(key);
    }

    @Override
    public void remove(String key) {
        map.remove(key);
    }

    @Override
    public boolean contains(String key) {
        return map.containsKey(key);
    }
}
