package com.joanzapata.android.kiss.api;

public interface BaseService {

    <T extends Message> T getCachedMessage(String key, Class<T> returnType);

    void cacheMessage(String key, Message object);

    void sendMessage(Message message);
}
