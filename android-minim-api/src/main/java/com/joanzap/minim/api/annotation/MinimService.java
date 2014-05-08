package com.joanzap.minim.api.annotation;

import com.joanzap.minim.api.BaseCache;
import com.joanzap.minim.api.internal.DefaultCache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface MinimService {

    Class<? extends BaseCache> cache() default DefaultCache.class;

}
