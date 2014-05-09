package com.joanzap.android.kiss.api.annotation;

import com.joanzap.android.kiss.api.BaseCache;
import com.joanzap.android.kiss.api.internal.DefaultCache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.CLASS)
@Target(ElementType.TYPE)
public @interface MinimService {

    Class<? extends BaseCache> cache() default DefaultCache.class;

}
