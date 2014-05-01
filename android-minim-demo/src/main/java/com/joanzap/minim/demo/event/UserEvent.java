package com.joanzap.minim.demo.event;

import com.joanzap.minim.api.BaseEvent;

public class UserEvent extends BaseEvent {

    public final Long id;

    public final String name;

    public final int age;

    public UserEvent(Long id, String name, int age) {
        this.id = id;
        this.name = name;
        this.age = age;
    }

}
