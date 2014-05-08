package com.joanzap.minim.internal;

import android.content.Context;
import android.util.Log;
import com.joanzap.minim.api.BaseCache;
import com.snappydb.SnappydbException;
import com.snappydb.internal.DBImpl;

import java.io.File;
import java.io.Serializable;

public class SnappyCache implements BaseCache {

    public static final boolean VERBOSE = true;

    private static final String TAG = SnappyCache.class.getSimpleName();

    static {
        System.loadLibrary("snappydb-native");
    }

    private final DBImpl dbImpl;

    @SuppressWarnings("ConstantConditions")
    public SnappyCache(Context context) {

        // Make the path at which we'll store the DB
        String path = context.getFilesDir().getAbsolutePath() + File.separator + "snappydb";

        // Create the DB, or throw if something is wrong
        try {
            dbImpl = new DBImpl(path);
        } catch (SnappydbException e) {
            throw new IllegalStateException("Could not initialize snappy database at " + path, e);
        }
    }

    @Override
    public void store(String key, Serializable object) {

        try {

            // Put the object in DB
            dbImpl.put(key, object);

        } catch (SnappydbException e) {

            // Don't make the app crash for a single failure, just log it
            if (VERBOSE) Log.w(TAG, "Was unable to store object " + object + " in cache.", e);
        }

    }

    @Override
    public <T extends Serializable> T get(String key, Class<T> expectedClass) {
        try {

            // Get the object in cache
            return dbImpl.get(key, expectedClass);

        } catch (SnappydbException e) {

            /*
             We don't call get if contains() return false, so an exception
             at this point probably means the class definition has changed,
             we should remove this obsolete value from the cache.
            */
            remove(key);
            return null;
        }

    }

    @Override
    public void remove(String key) {
        try {
            dbImpl.del(key);
        } catch (SnappydbException e) {
            // Just ignore it
        }

    }

    @Override
    public boolean contains(String key) {
        try {
            return dbImpl.exists(key);
        } catch (SnappydbException e) {
            if (VERBOSE) Log.w(TAG, "Was unable to check if key " + key + " is in cache.", e);
            return false;
        }
    }

}
