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
package com.joanzapata.android.asyncservice.api.internal;

import android.content.Context;
import com.joanzapata.android.asyncservice.api.Message;
import com.joanzapata.android.asyncservice.api.annotation.OnMessage;

import java.util.ArrayList;
import java.util.List;

import static com.joanzapata.android.asyncservice.api.annotation.OnMessage.Priority.*;

@SuppressWarnings("unchecked")
public final class AsyncService {

    static final List<Injector> injectors = new ArrayList<Injector>();

    public static Context context;

    /**
     * Inject @InjectService fields and activates
     * all @InjectResponse methods to receive further events.
     * @param object Inject the given object.
     */
    public static void inject(Object object) {

        // Extract context from given object if possible
        extractContextFromObject(object);

        // Try to find an injector for the supplied object class (or superclasses)
        Class<? extends Injector> injectorClass = null;
        Class currentClass = object.getClass();
        while (currentClass != null && (injectorClass = findInjectorFor(currentClass)) == null) {
            currentClass = currentClass.getSuperclass();
        }

        // If none, do nothing
        if (currentClass == null) return;

        // If an injector is found, use it
        try {
            Injector newInjector = injectorClass.newInstance();
            newInjector.setTarget(object);
            injectors.add(newInjector);
        } catch (Exception e) {
            throw new IllegalStateException("All injectorClasses should have a public no-arg constructor", e);
        }
    }

    /**
     * Prevent the given object to receive any
     * more message in its @OnMessage methods,
     * unless inject() is called again.
     */
    public static void unregister(Object object) {
        Injector injectorToRemove = null;
        for (Injector injector : injectors)
            if (injector.getTarget() == object)
                injectorToRemove = injector;

        if (injectorToRemove != null)
            injectors.remove(injectorToRemove);
    }

    /**
     * If the given object contains an Android context, extract
     * the application context and retain it statically.
     */
    private static void extractContextFromObject(Object object) {
        if (context == null && object instanceof Context) {
            context = ((Context) object).getApplicationContext();
        }
    }

    /**
     * Use class name to find a generated injector for the user class.
     * TODO This will cause problem with proguard, find better approach. (generated mapper ?)
     */
    private static Class<? extends Injector> findInjectorFor(Class currentClass) {
        try {
            return (Class<? extends Injector>) Class.forName(currentClass.getCanonicalName() + "Injector");
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /**
     * Send a message with no emitter, it will only be received
     * by @OnMessage(from = ALL) annotated methods.
     * @param message The message you want to send.
     */
    public static void dispatch(Object message) {
        Message internalMessage = new Message(message);
        dispatch(internalMessage);
    }

    /** Dispatch an event, application wide. */
    public static void dispatch(Message message) {
        dispatch(message, FIRST);
        dispatch(message, LAST);
    }

    private static void dispatch(Message message, OnMessage.Priority priority) {
        // Loop through injectors
        for (int i = 0; i < injectors.size(); i++) {
            Injector injector = injectors.get(i);

            // Dispatch event to it
            boolean isValid = injector.dispatch(message, priority);

            // Removed it from the list if the injector target is no more valid
            if (!isValid) injectors.remove(i--);
        }

    }

}
