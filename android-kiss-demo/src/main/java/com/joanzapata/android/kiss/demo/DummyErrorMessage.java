package com.joanzapata.android.kiss.demo;

import com.joanzapata.android.kiss.api.ErrorMessage;
import com.joanzapata.android.kiss.api.annotation.ThrowerParam;

public class DummyErrorMessage extends ErrorMessage {

    private Long id;

    public DummyErrorMessage(Throwable throwable, @ThrowerParam("id") Long id) {
        super(throwable);
        this.id = id;
    }
}
