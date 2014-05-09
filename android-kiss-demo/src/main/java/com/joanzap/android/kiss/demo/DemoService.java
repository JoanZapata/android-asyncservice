package com.joanzap.android.kiss.demo;

import com.joanzap.android.kiss.api.annotation.Cached;
import com.joanzap.android.kiss.api.annotation.KissService;
import com.joanzap.android.kiss.cache.snappyb.SnappyCache;
import com.joanzap.android.kiss.demo.event.UserEvent;

import static android.text.format.DateUtils.DAY_IN_MILLIS;
import static com.joanzap.android.kiss.api.annotation.Cached.Usage.CACHE_ONLY;

@KissService(cache = SnappyCache.class)
public class DemoService {

    /*
        By default, methods are executed in a background thread.
        No caching is involved.
    */
    public UserEvent getUserAsync(Long id) {
        return new UserEvent(id, "Joan", 25);
    }

    /*
        If you use @Cached, the result is cached before being sent to the receiver(s).
        On next call, receivers will immediately receive the previous cached event, then
        this method is called. Its new result replaces the previous cache, then is sent
        to the receivers.

        The default cache key is <class_name>.<method_name>(<arg1.toString>, <arg2.toString>, ...)
    */
    @Cached
    public UserEvent getUserAsyncWithCache(Long id) {
        sleep();
        return new UserEvent(id, "Joan", 25);
    }

    /*
        Same thing as above, except that this time, if a cached value
        is found, the cache is sent and this method is not called at all.
        This cache expires after one day. Passed this duration, on next
        call the cache is ignored and this method is called again.
     */
    @Cached(usage = CACHE_ONLY, validity = DAY_IN_MILLIS)
    public UserEvent getUserAsyncWithPersistingCache(Long id) {
        sleep();
        return new UserEvent(id, "Joan", 25);
    }

    // Private methods are not overridden, so you can call them directly.
    private void sleep() {
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
