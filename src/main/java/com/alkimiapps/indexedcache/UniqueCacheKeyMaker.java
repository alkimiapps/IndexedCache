/*
 * Copyright (c) 2019. Allan Boyd
 * This program is made available under the terms of the Apache License v2.0.
 */

package com.alkimiapps.indexedcache;

import javax.cache.Cache;

/**
 * A thing that makes unique keys (keys that will always miss on a call to Cache.get) for some cache.
 */
public interface UniqueCacheKeyMaker<K, V> {
    K makeUniqueCacheKeyForCache(Cache<K, V> cache);
}
