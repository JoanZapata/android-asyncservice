package com.joanzap.android.kiss.demo;

import com.joanzap.android.kiss.api.annotation.Cached;
import com.joanzap.android.kiss.api.annotation.MinimService;
import com.joanzap.android.kiss.demo.event.UserEvent;
import com.joanzap.android.kiss.cache.snappyb.SnappyCache;

@MinimService(cache = SnappyCache.class)
public class DemoService {

    @Cached
    public UserEvent getUser(Long id) {
        return new UserEvent(id, "Joan", 25);
    }

}
