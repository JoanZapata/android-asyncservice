package com.joanzap.minim.internal;

import com.joanzap.minim.api.BaseCache;

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
