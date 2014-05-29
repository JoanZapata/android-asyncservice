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
package com.joanzapata.android.kiss.api.internal;

import android.util.Log;
import com.snappydb.internal.DBImpl;

import java.io.File;
import java.io.Serializable;
import java.util.List;

import static java.util.Arrays.asList;

public final class KissCache {

    static {
        System.loadLibrary("snappydb-native");
    }

    /** Set it to false to avoid logs */
    public static final boolean VERBOSE = true;

    private static final String TAG = KissCache.class.getSimpleName();

    /** Can be null, initialized when the cache is used */
    private static DBImpl dbImpl;

    // Prevent instantiation
    private KissCache() {}

    private static boolean isReady() {
        if (dbImpl != null) return true;
        if (Kiss.context == null) return false;

        try {
            // Make the path at which we'll store the DB
            String path = Kiss.context.getFilesDir().getAbsolutePath() + File.separator + "kissdb";
            dbImpl = new DBImpl(path);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "Exception while initialiazing db", e);
            return false;
        }
    }

    public static void storeList(String key, List<? extends Serializable> object) {
        if (!isReady()) return;

        try {

            // Put the object in DB
            dbImpl.put(key, object.toArray());

        } catch (Exception e) {

            // Don't make the app crash for a single failure, just log it
            if (VERBOSE) Log.w(TAG, "Was unable to store object " + object + " in cache.", e);
        }
    }

    public static void store(String key, Serializable object) {
        if (!isReady()) return;

        try {

            // Put the object in DB
            dbImpl.put(key, object);

        } catch (Exception e) {

            // Don't make the app crash for a single failure, just log it
            if (VERBOSE) Log.w(TAG, "Was unable to store object " + object + " in cache.", e);
        }

    }

    public static <T extends Serializable> T get(String key, Class<T> expectedClass) {
        if (!isReady()) return null;
        try {

            // Get the object in cache
            return dbImpl.get(key, expectedClass);

        } catch (Exception e) {

            /*
             We don't call get if contains() return false, so an exception
             at this point probably means the class definition has changed,
             we should remove this obsolete value from the cache.
            */
            remove(key);
            return null;
        }

    }

    @SuppressWarnings("unchecked")
    public static <T extends Serializable> List<T> getList(String key, Class<T> expectedClass) {
        if (!isReady()) return null;
        try {

            // Get the object in cache
            T[] found = dbImpl.getArray(key, expectedClass);
            if (found == null) return null;
            return asList(found);

        } catch (Exception e) {

            /*
             We don't call get if contains() return false, so an exception
             at this point probably means the class definition has changed,
             we should remove this obsolete value from the cache.
            */
            remove(key);
            return null;
        }

    }

    public static void remove(String key) {
        if (!isReady()) return;
        try {
            dbImpl.del(key);
        } catch (Exception e) {
            // Just ignore it
        }

    }

    public static boolean contains(String key) {
        if (!isReady()) return false;

        try {
            return dbImpl.exists(key);
        } catch (Exception e) {
            if (VERBOSE) Log.w(TAG, "Was unable to check if key " + key + " is in cache.", e);
            return false;
        }
    }

}
