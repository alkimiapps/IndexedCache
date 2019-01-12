/*
 * Copyright (c) 2019. Allan Boyd
 * This program is made available under the terms of the Apache License v2.0.
 */

package com.alkimiapps.indexedcache.internal;

import com.googlecode.cqengine.IndexedCollection;

import javax.cache.configuration.CacheEntryListenerConfiguration;
import javax.cache.configuration.Factory;
import javax.cache.event.CacheEntryEventFilter;
import javax.cache.event.CacheEntryListener;

public final class IndexedCacheEntryListenerConfiguration<K, V> implements CacheEntryListenerConfiguration<K, V> {

    private IndexedCollection<V> indexedCollection;

    public IndexedCacheEntryListenerConfiguration(IndexedCollection<V> indexedCollection) {
        this.indexedCollection = indexedCollection;
    }
    @Override
    public Factory<CacheEntryListener<? super K, ? super V>> getCacheEntryListenerFactory() {
        return (Factory<CacheEntryListener<? super K, ? super V>>) () -> new IndexedCacheEntryListener<>(indexedCollection);
    }

    @Override
    public boolean isOldValueRequired() {
        return true;
    }

    @Override
    public Factory<CacheEntryEventFilter<? super K, ? super V>> getCacheEntryEventFilterFactory() {
        return (Factory<CacheEntryEventFilter<? super K, ? super V>>) AllInclusiveCacheEventFilter::new;
    }

    @Override
    public boolean isSynchronous() {
        return false;
    }
}
