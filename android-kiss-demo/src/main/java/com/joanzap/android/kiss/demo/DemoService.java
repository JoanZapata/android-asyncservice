package com.joanzap.android.kiss.demo;

import com.joanzap.android.kiss.api.annotation.Cached;
import com.joanzap.android.kiss.api.annotation.KissService;
import com.joanzap.android.kiss.demo.event.UserEvent;
import com.joanzap.android.kiss.cache.snappyb.SnappyCache;

@KissService(cache = SnappyCache.class)
public class DemoService {

    @Cached
    public UserEvent getUser(Long id) {
        return new UserEvent(id, "Joan", 25);
    }

}
