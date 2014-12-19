/**
 * Copyright 2014 Joan Zapata
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.joanzapata.android.asyncservice.demo;

import android.content.Context;
import android.widget.Toast;
import com.joanzapata.android.asyncservice.api.annotation.ApplicationContext;
import com.joanzapata.android.asyncservice.api.annotation.CacheThenCall;
import com.joanzapata.android.asyncservice.api.annotation.ErrorManagement;
import com.joanzapata.android.asyncservice.api.annotation.Init;
import com.joanzapata.android.asyncservice.api.annotation.AsyncService;
import com.joanzapata.android.asyncservice.api.annotation.Mapping;
import com.joanzapata.android.asyncservice.api.annotation.Null;
import com.joanzapata.android.asyncservice.api.annotation.Ui;
import com.joanzapata.android.asyncservice.demo.event.NoUserEvent;
import com.joanzapata.android.asyncservice.demo.event.UserEvent;

@AsyncService
public class DemoService {

    /*
        If you need a context you can inject
        the application context here.
    */
    @ApplicationContext
    static Context applicationContext;

    @Init
    static void initStatic() {
        // Executed once for all services
    }

    @Init
    static void init() {
        // Executed once for this service
    }

    /*
        By default, methods are executed in a background thread.
        No caching is involved.
    */
    @Null(NoUserEvent.class)
    @ErrorManagement(@Mapping(on = 200, send = DummyErrorMessage.class))
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
    @CacheThenCall
    public UserEvent getUserAsyncWithCache(Long id) {
        sleep();
        displayMessage("This is a toast displayed from the DemoService.");
        return new UserEvent(id, "Joan", 25);
    }

    @Ui
    protected void displayMessage(String message) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show();
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
