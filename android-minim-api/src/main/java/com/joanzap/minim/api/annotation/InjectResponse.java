package com.joanzap.minim.api.annotation;

import com.joanzap.minim.api.BaseEvent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.METHOD)
public @interface InjectResponse {

    Class<? extends BaseEvent> value() default USE_PARAMETER_TYPE.class;

    /** This is the default value for event(), do not use it. */
    static final class USE_PARAMETER_TYPE extends BaseEvent {}
}
