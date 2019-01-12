/*
 * Copyright (c) 2019. Allan Boyd
 * This program is made available under the terms of the Apache License v2.0.
 */

package com.alkimiapps.indexedcache.internal;

import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryEventFilter;
import javax.cache.event.CacheEntryListenerException;

/**
 * A CacheEntryEventFilter that cares about all kinds of CacheEntryEvent.
 * @param <K> the key type of the Cache
 * @param <V> the value type of the Cache
 */
public final class AllInclusiveCacheEventFilter<K, V> implements CacheEntryEventFilter<K, V> {
    @Override
    public boolean evaluate(CacheEntryEvent<? extends K, ? extends V> event) throws CacheEntryListenerException {
        return true;
    }
}
