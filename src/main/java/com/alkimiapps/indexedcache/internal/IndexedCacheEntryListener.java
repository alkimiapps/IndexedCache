/*
 * Copyright (c) 2019. Allan Boyd
 * This program is made available under the terms of the Apache License v2.0.
 */

package com.alkimiapps.indexedcache.internal;

import com.googlecode.cqengine.IndexedCollection;

import javax.cache.event.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Implements CacheEntryExpiredListener and CacheEntryRemovedListener so that when an entry is removed from the cache
 * i.e. because it has timed out or is removed for any other reason then this change is reflected in the associated
 * IndexedCollection. This ensures that the characteristics of the cache in terms of expiry policy are supported by the
 * IndexedCollection.
 *
 * Implements CacheEntryUpdatedListener and CacheEntryCreatedListener so that items updated/added to the cache independently
 * of the IndexedCache are also updated/added to the IndexedCache.
 */
public final class IndexedCacheEntryListener<K, V> implements CacheEntryExpiredListener<K, V>, CacheEntryRemovedListener<K, V>, CacheEntryUpdatedListener<K, V>, CacheEntryCreatedListener<K, V> {

    private IndexedCollection<V> indexedCollection;

    IndexedCacheEntryListener(IndexedCollection<V> indexedCollection) {
        this.indexedCollection = indexedCollection;
    }

    @Override
    public void onExpired(Iterable<CacheEntryEvent<? extends K, ? extends V>> cacheEntryEvents) throws CacheEntryListenerException {
        cacheEntryEvents.forEach(cacheEntryEvent -> indexedCollection.remove(cacheEntryEvent.getValue()));
    }

    @Override
    public void onRemoved(Iterable<CacheEntryEvent<? extends K, ? extends V>> cacheEntryEvents) throws CacheEntryListenerException {
        cacheEntryEvents.forEach(cacheEntryEvent -> indexedCollection.remove(cacheEntryEvent.getValue()));
    }

    @Override
    public void onCreated(Iterable<CacheEntryEvent<? extends K, ? extends V>> cacheEntryEvents) throws CacheEntryListenerException {
        cacheEntryEvents.forEach(cacheEntryEvent -> indexedCollection.add(cacheEntryEvent.getValue()));
    }

    @Override
    public void onUpdated(Iterable<CacheEntryEvent<? extends K, ? extends V>> cacheEntryEvents) throws CacheEntryListenerException {
        List<V> newValues = new ArrayList<>();
        List<V> oldValues = new ArrayList<>();
        cacheEntryEvents.forEach(entry -> newValues.add(entry.getValue()));
        cacheEntryEvents.forEach(entry -> oldValues.add(entry.getOldValue()));
        indexedCollection.update(oldValues, newValues);
    }
}
