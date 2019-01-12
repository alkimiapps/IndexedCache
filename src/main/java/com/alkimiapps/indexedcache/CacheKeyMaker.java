/*
 * Copyright (c) 2019. Allan Boyd
 * This program is made available under the terms of the Apache License v2.0.
 */

package com.alkimiapps.indexedcache;

/**
 * A thing that makes cache keys for values.
 */
public interface CacheKeyMaker<K, V> {
    /**
     * Return the key for some value. This method must always return a consistent result i.e. a given value must
     * always have the same (equal) key.
     */
    K makeKey(V value);
}
