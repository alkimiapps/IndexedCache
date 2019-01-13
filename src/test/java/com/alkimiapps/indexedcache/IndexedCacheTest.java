/*
 * Copyright (c) 2019. Allan Boyd
 * This program is made available under the terms of the Apache License v2.0.
 */

package com.alkimiapps.indexedcache;

import com.alkimiapps.async.Waiter;
import com.alkimiapps.keys.Widget;
import com.alkimiapps.mxbean.CacheStatsProvider;
import com.googlecode.cqengine.ConcurrentIndexedCollection;
import com.googlecode.cqengine.IndexedCollection;
import com.googlecode.cqengine.attribute.Attribute;
import com.googlecode.cqengine.index.radixreversed.ReversedRadixTreeIndex;
import com.googlecode.cqengine.query.Query;
import com.googlecode.cqengine.resultset.ResultSet;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.management.CacheStatisticsMXBean;
import javax.cache.spi.CachingProvider;

import static com.googlecode.cqengine.query.QueryFactory.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;

public class IndexedCacheTest {

    private static final long CACHE_TTL_MILLIS = 50L;
    private IndexedCache<Widget, Widget> indexedCache;
    private Cache<Widget, Widget> cache;
    private CacheKeyMaker<Widget, Widget> cacheKeyMaker = new IdentityCacheKeyMaker<>();
    private static final Attribute<Widget, String> Widget_Name = attribute("widgetName", Widget::getName);

    @Before
    public void setup() {

        CachingProvider provider = Caching.getCachingProvider();
        CacheManager cacheManager = provider.getCacheManager();
        MutableConfiguration<Widget, Widget> configuration =
                new MutableConfiguration<Widget, Widget>()
                        .setStatisticsEnabled(true)
                        .setTypes(Widget.class, Widget.class)
                        .setStoreByValue(false)
                        .setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(new Duration(MILLISECONDS, CACHE_TTL_MILLIS)));
        cache = cacheManager.createCache("jCache", configuration);
        IndexedCollection<Widget> indexedCollection = new ConcurrentIndexedCollection<>();
        indexedCache = new IndexedCache<>(indexedCollection, cache, cacheKeyMaker);
        indexedCache.addIndex(ReversedRadixTreeIndex.onAttribute(Widget_Name));
    }

    @After
    public void tearDown() {
        cache.close();
    }

    @Test
    public void testGetCache() {
        assertEquals(cache, indexedCache.getCache());
    }

    @Test
    public void add() {
        Widget widget = new Widget("Frank");
        indexedCache.add(widget);
        Query<Widget> query = equal(Widget_Name, "Frank");
        ResultSet<Widget> result = indexedCache.retrieve(query);
        assertEquals(1, result.size());
        assertEquals("Frank", result.iterator().next().getName());
        assertEquals("Frank", cache.get(cacheKeyMaker.makeKey(widget)).getName());
    }

    @Test
    public void testRetrieveHits() {

        Widget frank = new Widget("Frank");
        indexedCache.add(frank);

        Widget bob = new Widget("Bob");
        indexedCache.add(bob);

        Widget jane = new Widget("Jane");
        indexedCache.add(jane);

        Query<Widget> query = or(endsWith(Widget_Name, "ank"), startsWith(Widget_Name, "Bo"));
        ResultSet<Widget> results = indexedCache.retrieve(query);

        assertEquals(2, results.size());

        CacheStatisticsMXBean stats = CacheStatsProvider.getCacheStatisticsMXBean(indexedCache.getCache().getName());
        assertNotNull(stats);
        Waiter.waitForValueWithTimeout(() -> {
            if (stats.getCacheHits() > 0) {
                return stats.getCacheHits();
            } else {
                return null;
            }
        }, 2L);
        assertEquals(2, stats.getCacheHits());
        assertEquals(0, stats.getCacheMisses());
    }

    @Test
    public void testRetrieveMiss() {

        Widget frank = new Widget("Frank");
        indexedCache.add(frank);

        Widget bob = new Widget("Bob");
        indexedCache.add(bob);

        Widget jane = new Widget("Jane");
        indexedCache.add(jane);

        Query<Widget> query = or(endsWith(Widget_Name, "xxx"), startsWith(Widget_Name, "xx"));
        ResultSet<Widget> results = indexedCache.retrieve(query);

        assertEquals(0, results.size());

        CacheStatisticsMXBean stats = CacheStatsProvider.getCacheStatisticsMXBean(indexedCache.getCache().getName());
        assertNotNull(stats);
        Waiter.waitForValueWithTimeout(() -> {
            if (stats.getCacheMisses() > 0) {
                return stats.getCacheMisses();
            } else {
                return null;
            }
        }, 1L);
        assertEquals(0, stats.getCacheHits());
        assertEquals(1, stats.getCacheMisses());
    }

//    private Widget testCreate() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
//        Class<com.alkimiapps.indexedcache.IndexedCacheTest.Widget> widgetClass = Class.forName("com.alkimiapps.indexedcache.IndexedCacheTest.Widget");
//        Widget instance = (Widget) Class.forName("com.alkimiapps.indexedcache.IndexedCacheTest.Widget").newInstance();
//        return instance;
//    }
}
