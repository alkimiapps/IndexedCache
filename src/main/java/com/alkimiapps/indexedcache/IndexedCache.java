/*
 * Copyright (c) 2019. Allan Boyd
 * This program is made available under the terms of the Apache License v2.0.
 */

package com.alkimiapps.indexedcache;

import com.alkimiapps.indexedcache.internal.CacheMaintainer;
import com.alkimiapps.indexedcache.internal.IndexedCacheEntryListenerConfiguration;
import com.alkimiapps.indexedcache.internal.SubclassableClassUniqueCacheKeyMaker;
import com.googlecode.cqengine.IndexedCollection;
import com.googlecode.cqengine.index.Index;
import com.googlecode.cqengine.query.Query;
import com.googlecode.cqengine.query.option.QueryOptions;
import com.googlecode.cqengine.resultset.ResultSet;

import javax.cache.Cache;
import java.util.Collection;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * A com.googlecode.cqengine.IndexedCollection that marries CQEngine with JCache to provide an indexed cache i.e.
 * a cache on which indexes can be applied to perform fast complex queries as well as providing cache characteristics
 * such as expiry and statistics.
 *
 * @param <K> cache key type
 * @param <V> cache value type
 */
public final class IndexedCache<K, V> implements IndexedCollection<V> {

    private IndexedCollection<V> indexedCollection;
    private CacheMaintainer<K, V> cacheMaintainer;
    private Cache<K, V> cache;

    /**
     * Make a new IndexedCache based on an com.googlecode.cqengine.IndexedCollection and a javax.cache.Cache.
     *
     * This constructor is only suitable when you want to inject the cache and the class of the cache key is non-final
     * and has at least one public or protected constructor.
     *
     * Changes in the contents of the javax.cache.Cache performed outside this class are reflected in the
     * com.googlecode.cqengine.IndexedCollection.
     *
     * Changes in the contents of the IndexedCollection performed outside of this class are NOT reflected in the
     * javax.cache.Cache - because CQEngine does not provide a public API to observe changes in an IndexedCollection.
     *
     * Allowing both the IndexedCollection and the Cache to be defined externally provides flexibility but - in the case
     * of the IndexedCollection - the cost of not encapsulating is the risk that the IndexedCollection and the
     * Cache could get out of sync. It is therefore recommended that the provided IndexedCollection APIs are NOT used
     * independently of this IndexedCache (there should be no need because the IndexedCache is an IndexedCollection).
     *
     * To recap: it is safe to add/remove/update entries in the Cache outside of this class if necessary. It is NOT
     * safe to add/remove/update entries in the provided IndexedCollection outside of this class.
     */
    public IndexedCache(IndexedCollection<V> indexedCollection, Cache<K, V> cache, CacheKeyMaker<K, V> cacheKeyMaker) {
        this(indexedCollection, cache, cacheKeyMaker, new SubclassableClassUniqueCacheKeyMaker<>());
    }

    /**
     * Make a new IndexedCache based on an com.googlecode.cqengine.IndexedCollection and a javax.cache.Cache.
     *
     * If your cache key class is a final class or has no public constructors and you want to inject the cache then
     * use this constructor and provide a UniqueCacheKeyMaker that can make a unique key for your cache.
     *
     * Changes in the contents of the javax.cache.Cache performed outside this class are reflected in the
     * com.googlecode.cqengine.IndexedCollection.
     *
     * Changes in the contents of the IndexedCollection performed outside of this class are NOT reflected in the
     * javax.cache.Cache - because CQEngine does not provide a public API to observe changes in an IndexedCollection.
     *
     * Allowing both the IndexedCollection and the Cache to be defined externally provides flexibility but - in the case
     * of the IndexedCollection - the cost of not encapsulating is the risk that the IndexedCollection and the
     * Cache could get out of sync. It is therefore recommended that the provided IndexedCollection APIs are NOT used
     * independently of this IndexedCache (there should be no need because the IndexedCache is an IndexedCollection).
     *
     * To recap: it is safe to add/remove/update entries in the Cache outside of this class if necessary. It is NOT
     * safe to add/remove/update entries in the provided IndexedCollection outside of this class.
     */
    public IndexedCache(IndexedCollection<V> indexedCollection, Cache<K, V> cache, CacheKeyMaker<K, V> cacheKeyMaker, UniqueCacheKeyMaker<K> uniqueCacheKeyMaker) {
        this.indexedCollection = indexedCollection;
        this.cache = cache;
        this.cacheMaintainer = new CacheMaintainer<>(cache, cacheKeyMaker, uniqueCacheKeyMaker);
        cache.registerCacheEntryListener(new IndexedCacheEntryListenerConfiguration<>(indexedCollection));
    }

    public Cache getCache() {
        return cache;
    }

    @Override
    public ResultSet<V> retrieve(Query<V> query) {
        ResultSet<V> resultSet = indexedCollection.retrieve(query);
        if (resultSet.size() > 0) { // todo only do cache hit/miss if the cache stats are enabled
            cacheMaintainer.registerCacheHits(resultSet);
        } else {
            cacheMaintainer.registerCacheMiss();
        }
        return resultSet;
    }

    @Override
    public ResultSet<V> retrieve(Query<V> query, QueryOptions queryOptions) {
        ResultSet<V> resultSet = indexedCollection.retrieve(query, queryOptions);
        if (resultSet.size() > 0) { // todo only do cache hit/miss if the cache stats are enabled
            cacheMaintainer.registerCacheHits(resultSet);
        } else {
            cacheMaintainer.registerCacheMiss();
        }
        return resultSet;
    }

    @Override
    public boolean update(Iterable<V> objectsToRemove, Iterable<V> objectsToAdd) {
        boolean updated = indexedCollection.update(objectsToRemove, objectsToAdd);
        if (updated) {
            cacheMaintainer.indexCollectionWasUpdated(objectsToRemove, objectsToAdd);
        }
        return updated;
    }

    @Override
    public boolean update(Iterable<V> objectsToRemove, Iterable<V> objectsToAdd, QueryOptions queryOptions) {
        boolean updated = indexedCollection.update(objectsToRemove, objectsToAdd, queryOptions);
        if (updated) {
            cacheMaintainer.indexCollectionWasUpdated(objectsToRemove, objectsToAdd);
        }
        return updated;
    }

    @Override
    public void addIndex(Index<V> index) {
        indexedCollection.addIndex(index);
    }

    @Override
    public void addIndex(Index<V> index, QueryOptions queryOptions) {
        indexedCollection.addIndex(index, queryOptions);
    }

    @Override
    public int size() {
        return indexedCollection.size();
    }

    @Override
    public boolean isEmpty() {
        return indexedCollection.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return indexedCollection.contains(o);
    }

    @Override
    public Iterator<V> iterator() {
        return indexedCollection.iterator();
    }

    @Override
    public Object[] toArray() {
        return indexedCollection.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return indexedCollection.toArray(a);
    }

    @Override
    public boolean add(V v) {
        boolean added = indexedCollection.add(v);
        if (added) {
            cacheMaintainer.objectWasAdded(v);
        }

        return added;
    }

    @Override
    public boolean remove(Object o) {
        boolean removed = indexedCollection.remove(o);
        if (removed) {
            // According to IndexedCollection docs, if o is not a V then it will throw an exception meaning
            // the following cast is safe.
            cacheMaintainer.objectWasRemoved((V) o);
        }

        return removed;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return indexedCollection.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends V> c) {
        boolean added = indexedCollection.addAll(c);
        if(added){
            cacheMaintainer.indexCollectionWasUpdated(null, c);
        }

        return added;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return indexedCollection.removeAll(c);
    }


    @Override
    public void clear() {
        indexedCollection.clear();
        cache.clear(); // Calling clear also resets the stats - i.e. as well as removing stuff
    }

    @Override
    public boolean removeIf(Predicate<? super V> filter) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Stream<V> stream() {
        return indexedCollection.stream();
    }

    @Override
    public Stream<V> parallelStream() {
        return indexedCollection.parallelStream();
    }

    @Override
    public void forEach(Consumer<? super V> action) {
        indexedCollection.forEach(action);
    }

    @Override
    public Iterable<Index<V>> getIndexes() {
        return indexedCollection.getIndexes();
    }
}