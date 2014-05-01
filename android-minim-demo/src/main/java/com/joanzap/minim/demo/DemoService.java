package com.joanzap.minim.demo;

import com.joanzap.minim.api.annotation.Cached;
import com.joanzap.minim.api.annotation.MinimService;
import com.joanzap.minim.demo.event.UserEvent;

@MinimService
public class DemoService {

    @Cached
    public UserEvent getUser(Long id) {
        return new UserEvent(id, "Joan", 25);
    }

}
