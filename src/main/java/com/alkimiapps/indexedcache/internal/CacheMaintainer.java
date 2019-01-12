/*
 * Copyright (c) 2019. Allan Boyd
 * This program is made available under the terms of the Apache License v2.0.
 */

package com.alkimiapps.indexedcache.internal;

import com.alkimiapps.indexedcache.CacheKeyMaker;
import com.alkimiapps.indexedcache.UniqueCacheKeyMaker;
import com.googlecode.cqengine.resultset.ResultSet;

import javax.cache.Cache;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * The purpose of the CacheMaintainer is to allow a javax.cache.Cache in-sync with an IndexedCollection.
 * This is necessary in order to support cache statistics and to ensure that objects added or removed from an associated
 * IndexedCollection are also added/removed from the Cache.
 */
public final class CacheMaintainer<K, V> {
    private Cache<K, V> cache;
    private CacheKeyMaker<K, V> cacheKeyMaker;
    private UniqueCacheKeyMaker<K, V> uniqueCacheKeyMaker;

    private static final int DEFAULT_CORE_THREADS = 1;
    private static final int DEFAULT_MAX_THREADS = 1;
    private static final long DEFAULT_THREAD_KEEP_ALIVE_SECONDS = 0;
    private BlockingQueue<Runnable> queue = new LinkedBlockingDeque<>();

    private ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(DEFAULT_CORE_THREADS, DEFAULT_MAX_THREADS, DEFAULT_THREAD_KEEP_ALIVE_SECONDS, TimeUnit.SECONDS, queue);

    public CacheMaintainer(Cache<K, V> cache, CacheKeyMaker<K, V> cacheKeyMaker, UniqueCacheKeyMaker<K, V> uniqueCacheKeyMaker) {
        this.cache = cache;
        this.cacheKeyMaker = cacheKeyMaker;
        this.uniqueCacheKeyMaker = uniqueCacheKeyMaker;
    }

    public void registerCacheHits(ResultSet<V> resultSet) {
        // We need to hit the cache so that its stats will be maintained and we can do this on a background
        // thready so that the resultSet can be returned to the caller without having to wait for this method
        // to complete
        threadPoolExecutor.execute(() -> resultSet.stream()
                .forEach(v -> cache.get(cacheKeyMaker.makeKey(v))));
    }

    public void registerCacheMiss() {
        // Registering a cache miss is a bit tricky because we need to create an instance of a key that will
        // not be in the cache in order to generate a cache miss. That's when a NonFinalClassUniqueCacheKeyMaker comes in handy.
        threadPoolExecutor.execute(() -> {
            K uniqueCacheKey = uniqueCacheKeyMaker.makeUniqueCacheKeyForCache(cache);
            if (cache.get(uniqueCacheKey) != null) {
                throw new RuntimeException("Failed to generate a cache miss with cache key: " + uniqueCacheKey);
            }
        });
    }

    public void indexCollectionWasUpdated(Iterable<? extends V> objectsRemoved, Iterable<? extends V> objectsAdded) {
        // Remove first so that there is more room if needed
        if (objectsRemoved != null) {
            objectsRemoved.forEach(v -> cache.remove(cacheKeyMaker.makeKey(v), v));
        }
        if (objectsAdded != null) {
            objectsAdded.forEach(v -> cache.put(cacheKeyMaker.makeKey(v), v));
        }
    }

    public void objectWasAdded(V v) {
        cache.put(cacheKeyMaker.makeKey(v), v);
    }

    public void objectWasRemoved(V v) {
        cache.remove(cacheKeyMaker.makeKey(v));
    }
}
