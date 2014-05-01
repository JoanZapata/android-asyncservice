package com.joanzap.minim.demo.event;

import com.joanzap.minim.api.BaseEvent;

public class UserEvent extends BaseEvent {

    public final String name;

    public final int age;

    public UserEvent(String name, int age) {
        this.name = name;
        this.age = age;
    }

}
