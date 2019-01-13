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
import com.googlecode.cqengine.query.option.DeduplicationStrategy;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.alkimiapps.cache.CacheInfo.cacheEntryCount;
import static com.googlecode.cqengine.query.QueryFactory.*;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
        if (!cache.isClosed()) {
            cache.close();
        }
    }

    // todo test add/remove from cache also adds/removes from indexedCache
    // todo test expiry
    // todo set no stats when stats disabled

    @Test
    public void testGetCache() {
        assertEquals(cache, indexedCache.getCache());
    }

    @Test
    public void testAdd() {
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

        testCacheOnlyHits(2);
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
        indexedCache.retrieve(query);
        indexedCache.retrieve(query);
        CacheStatisticsMXBean stats = CacheStatsProvider.getCacheStatisticsMXBean(indexedCache.getCache().getName());
        assertNotNull(stats);
        Waiter.waitForValueWithTimeout(() -> {
            if (stats.getCacheMisses() == 3) {
                return stats.getCacheMisses();
            } else {
                return null;
            }
        });
        assertEquals(0, stats.getCacheHits());
        assertEquals(3, stats.getCacheMisses());
    }

    @Test
    public void testRetrieveWithOrderingOption() {

        Widget frank = new Widget("Frank");
        indexedCache.add(frank);

        Widget bob = new Widget("Bob");
        indexedCache.add(bob);

        Widget jane = new Widget("Jane");
        indexedCache.add(jane);

        Query<Widget> query = or(contains(Widget_Name, "n"), startsWith(Widget_Name, "Bo"));
        ResultSet<Widget> results = indexedCache.retrieve(query, queryOptions(orderBy(ascending(Widget_Name))));

        assertEquals(3, results.size());
        assertEquals("Bob", results.stream().findFirst().orElse(new Widget("not found")).getName());
        assertEquals("Frank", results.stream().skip(1).findFirst().orElse(new Widget("not found")).getName());
        assertEquals("Jane", results.stream().skip(2).findFirst().orElse(new Widget("not found")).getName());

        testCacheOnlyHits(3);
    }

    @Test
    public void testUpdateAddingAndRemovingObjects() {
        Widget frank = new Widget("Frank");
        Widget bob = new Widget("Bob");
        Widget jane = new Widget("Jane");

        List<Widget> widgets = Arrays.asList(frank, bob, jane);

        // Adding
        indexedCache.update(Collections.emptyList(), widgets);
        assertEquals(3, cacheEntryCount(indexedCache.getCache()));
        testCacheContents(widgets);

        // Test idempotency...
        indexedCache.update(Collections.emptyList(), widgets);
        assertEquals(3, cacheEntryCount(indexedCache.getCache()));
        testCacheContents(widgets);

        // Removing
        indexedCache.update(widgets, Collections.emptyList());
        assertEquals(0, cacheEntryCount(indexedCache.getCache()));

        // Test idempotency...
        indexedCache.update(widgets, Collections.emptyList());
        assertEquals(0, cacheEntryCount(indexedCache.getCache()));
    }

    @Test
    public void testUpdateAddingAndRemovingObjectsAtTheSameTime() {
        Widget frank = new Widget("Frank");
        Widget bob = new Widget("Bob");
        Widget jane = new Widget("Jane");
        List<Widget> widgetsToAdd = Arrays.asList(frank, bob, jane);

        Widget dave = new Widget("Dave");
        Widget sally = new Widget("Sally");
        Widget fred = new Widget("Fred");
        List<Widget> widgetsToRemove = Arrays.asList(dave, sally, fred);

        // Add objects to remove
        indexedCache.update(Collections.emptyList(), widgetsToRemove);

        // Remove and add
        indexedCache.update(widgetsToRemove, widgetsToAdd);
        assertEquals(3, cacheEntryCount(indexedCache.getCache()));
        testCacheContents(widgetsToAdd);
    }

    @Test
    public void testUpdateAddingAndRemovingObjectsWithOptions() {
        Widget frank = new Widget("Frank");
        Widget jane = new Widget("Jane");
        Widget bob = new Widget("Bob");

        List<Widget> widgets = Arrays.asList(frank, bob, jane);

        indexedCache.update(Collections.emptyList(), widgets, queryOptions(enableFlags("flag")));
        testCacheContents(widgets);
        assertEquals(3, cacheEntryCount(indexedCache.getCache()));

        Query<Widget> query = or(contains(Widget_Name, "n"), startsWith(Widget_Name, "Bo"));
        ResultSet<Widget> results = indexedCache.retrieve(query);

        assertEquals(3, results.size());
    }

    @Test
    public void testSize() {
        Widget frank = new Widget("Frank");
        Widget jane = new Widget("Jane");
        Widget bob = new Widget("Bob");

        List<Widget> widgets = Arrays.asList(frank, bob, jane);
        indexedCache.update(Collections.emptyList(), widgets);
        assertEquals(3, indexedCache.size());
    }

    private void testCacheOnlyHits(int expectedHitCount) {

        CacheStatisticsMXBean stats = CacheStatsProvider.getCacheStatisticsMXBean(indexedCache.getCache().getName());
        assertNotNull(stats);
        Waiter.waitForValueWithTimeout(() -> {
            if (stats.getCacheHits() == expectedHitCount) {
                return stats.getCacheHits();
            } else {
                return null;
            }
        });
        assertEquals(expectedHitCount, stats.getCacheHits());
        assertEquals(0, stats.getCacheMisses());
    }

    private void testCacheContents(List<Widget> list) {
        assertTrue(indexedCache.containsAll(list));
        for (Widget widget: list) {
            Widget key = cacheKeyMaker.makeKey(widget);
            assertEquals(widget, cache.get(key));
        }
    }
}
