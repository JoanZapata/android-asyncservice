package com.joanzap.android.kiss.api.annotation;

import com.joanzap.android.kiss.api.BaseEvent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface Result {

    Class<? extends BaseEvent> value() default USE_PARAMETER_TYPE.class;

    Sender from() default Sender.THIS;

    /** This is the default value for event(), do not use it. */
    static final class USE_PARAMETER_TYPE extends BaseEvent {}

    static enum Sender {
        THIS, ALL
    }
}
