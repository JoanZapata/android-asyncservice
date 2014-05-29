package com.joanzapata.android.kiss.demo;

import com.joanzapata.android.kiss.api.annotation.ThrowerParam;

public class DummyErrorMessage {

    private Throwable throwable;
    private Long id;

    public DummyErrorMessage(Throwable throwable, @ThrowerParam("id") Long id) {
        this.throwable = throwable;
        this.id = id;
    }
}
