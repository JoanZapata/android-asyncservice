package com.joanzap.minim.api;

import java.io.Serializable;

public interface BaseCache {

    void store(String key, Serializable object);

    <T extends Serializable> T get(String key, Class<T> expectedClass);

    void remove(String key);

    boolean contains(String key);

}
