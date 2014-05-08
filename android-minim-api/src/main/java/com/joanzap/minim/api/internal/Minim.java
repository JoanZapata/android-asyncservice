package com.joanzap.minim.api.internal;

import com.joanzap.minim.api.BaseEvent;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unchecked")
public final class Minim {

    static final List<Injector> injectors = new ArrayList<Injector>();

    /**
     * Inject @InjectService fields and activates
     * all @InjectResponse methods to receive further events.
     * @param object Inject the given object.
     */
    public static void inject(Object object) {

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

    private static Class<? extends Injector> findInjectorFor(Class currentClass) {
        try {
            return (Class<? extends Injector>) Class.forName(currentClass.getCanonicalName() + "Injector");
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    /** Dispatch an event, application wide. */
    public static void dispatch(BaseEvent event) {

        // Loop through injectors
        for (int i = 0; i < injectors.size(); i++) {
            Injector injector = injectors.get(i);

            // Dispatch event to it
            boolean isValid = injector.dispatch(event);

            // Removed it from the list if the injector target is no more valid
            if (!isValid) injectors.remove(i--);
        }
    }

}
