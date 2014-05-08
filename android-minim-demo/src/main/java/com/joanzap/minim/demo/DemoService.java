package com.joanzap.minim.demo;

import com.joanzap.minim.api.annotation.Cached;
import com.joanzap.minim.api.annotation.MinimService;
import com.joanzap.minim.demo.event.UserEvent;
import com.joanzap.minim.internal.SnappyCache;

@MinimService(cache = SnappyCache.class)
public class DemoService {

    @Cached
    public UserEvent getUser(Long id) {
        return new UserEvent(id, "Joan", 25);
    }

}
