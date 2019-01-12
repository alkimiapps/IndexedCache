/*
 * Copyright (c) 2019. Allan Boyd
 * This program is made available under the terms of the Apache License v2.0.
 */

package com.alkimiapps.indexedcache;

/**
 * A CacheKeyMaker whose keys are the same as their associated values.
 *
 * This CacheKeyMaker is useful for IndexedCache instances where the cache key can be the same type as the cache value
 * of the IndexedCache but only when the value type overrides Object.hashCode() and Object.equals() such that
 * both Object.hashCode() and Object.equals() resolve to the same values for value instances that are meant to be
 * considered equal (otherwise caching behaviour will not likely work as expected)
 */
public class IdentityCacheKeyMaker<V> implements CacheKeyMaker<V, V> {
    @Override
    public V makeKey(V value) {
        return value;
    }
}
