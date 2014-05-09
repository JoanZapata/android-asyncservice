package com.joanzap.android.kiss.demo.event;

import com.joanzap.android.kiss.api.BaseEvent;

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
