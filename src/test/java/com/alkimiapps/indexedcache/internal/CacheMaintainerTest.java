/*
 * Copyright (c) 2019. Allan Boyd
 * This program is made available under the terms of the Apache License v2.0.
 */

package com.alkimiapps.indexedcache.internal;

import com.alkimiapps.async.Waiter;
import com.alkimiapps.indexedcache.CacheKeyMaker;
import com.alkimiapps.indexedcache.IdentityCacheKeyMaker;
import com.alkimiapps.keys.Widget;
import com.alkimiapps.mxbean.CacheStatsProvider;
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

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CacheMaintainerTest {

    private Cache<Widget, Widget> cacheWithObjectKeys;
    private CacheMaintainer<Widget, Widget> widgetStringCacheMaintainer;
    private CacheKeyMaker<Widget, Widget> widgetCacheKeyMaker = new IdentityCacheKeyMaker<>();

    @Before
    public void setup() {

        CachingProvider provider = Caching.getCachingProvider();
        CacheManager cacheManager = provider.getCacheManager();

        MutableConfiguration<Widget, Widget> configurationForCacheWithWidgetKeys =
                new MutableConfiguration<Widget, Widget>()
                        .setStatisticsEnabled(true)
                        .setTypes(Widget.class, Widget.class)
                        .setStoreByValue(false)
                        .setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.ONE_MINUTE));
        cacheWithObjectKeys = cacheManager.createCache("widgetKeyJCache", configurationForCacheWithWidgetKeys);
        widgetStringCacheMaintainer = new CacheMaintainer<>(cacheWithObjectKeys, widgetCacheKeyMaker, new SubclassableClassUniqueCacheKeyMaker<>());

    }

    @After
    public void tearDown() {
        cacheWithObjectKeys.close();
    }

    @Test
    public void testRegisterCacheHitsForObjectKeyCache() throws InterruptedException {
        List<Widget> values = Arrays.asList(new Widget("Bob"), new Widget("Sally"), new Widget("Jane"));
        values.forEach(v -> widgetStringCacheMaintainer.objectWasAdded(v));
        CacheStatisticsMXBean stats = CacheStatsProvider.getCacheStatisticsMXBean(cacheWithObjectKeys.getName());
        assertNotNull(stats);
        assertEquals(0, stats.getCacheHits());
        assertEquals(0, stats.getCacheMisses());
        ResultSet<Widget> resultSet = mock(ResultSet.class);
        when(resultSet.stream()).thenReturn(values.stream());
        when(resultSet.iterator()).thenReturn(values.iterator());
        when(resultSet.size()).thenReturn(values.size());
        widgetStringCacheMaintainer.registerCacheHits(resultSet);
        Waiter.waitForValueWithTimeout(() -> {
            if (stats.getCacheHits() > 0) {
                return stats.getCacheHits();
            } else {
                return null;
            }
        }, 3L);
        assertEquals(3, stats.getCacheHits());
        assertEquals(0, stats.getCacheMisses());
    }

    @Test
    public void testRegisterCacheMissForObjectKeyCache() {
        // todo
        List<Widget> values = Collections.emptyList();
        CacheStatisticsMXBean stats = CacheStatsProvider.getCacheStatisticsMXBean(cacheWithObjectKeys.getName());
        assertNotNull(stats);
        assertEquals(0, stats.getCacheHits());
        assertEquals(0, stats.getCacheMisses());
        ResultSet<Widget> resultSet = mock(ResultSet.class);
        when(resultSet.stream()).thenReturn(values.stream());
        when(resultSet.iterator()).thenReturn(values.iterator());
        widgetStringCacheMaintainer.registerCacheMiss();
        Waiter.waitForValueWithTimeout(() -> {
            if (stats.getCacheMisses() > 0) {
                return stats.getCacheMisses();
            } else {
                return null;
            }
        }, 1L);
        assertEquals(1, stats.getCacheMisses());
        assertEquals(0, stats.getCacheHits());
    }

    @Test
    public void testIndexCollectionWasUpdatedWithAllRemoved() {
        List<Widget> values = Arrays.asList(new Widget("Bob"), new Widget("Sally"), new Widget("Jane"));
        values.forEach(v -> widgetStringCacheMaintainer.objectWasAdded(v));

        widgetStringCacheMaintainer.indexCollectionWasUpdated(values, null);
        assertEquals(0, cacheEntryCount());
    }

    @Test
    public void testIndexCollectionWasUpdatedWithOnlyAdded() {
        List<Widget> values = Arrays.asList(new Widget("Bob"), new Widget("Sally"), new Widget("Jane"));

        widgetStringCacheMaintainer.indexCollectionWasUpdated(null, values);
        assertEquals(3, cacheEntryCount());
        assertEquals(new Widget("Bob"), cacheWithObjectKeys.get(widgetCacheKeyMaker.makeKey(new Widget("Bob"))));
        assertEquals(new Widget("Sally"), cacheWithObjectKeys.get(widgetCacheKeyMaker.makeKey(new Widget("Sally"))));
        assertEquals(new Widget("Jane"), cacheWithObjectKeys.get(widgetCacheKeyMaker.makeKey(new Widget("Jane"))));
    }

    @Test
    public void testIndexCollectionWasUpdatedIdempotency() {
        List<Widget> values = Arrays.asList(new Widget("Bob"), new Widget("Sally"), new Widget("Jane"));

        widgetStringCacheMaintainer.indexCollectionWasUpdated(values, values);
        widgetStringCacheMaintainer.indexCollectionWasUpdated(values, values);

        assertEquals(new Widget("Bob"), cacheWithObjectKeys.get(widgetCacheKeyMaker.makeKey(new Widget("Bob"))));
        assertEquals(new Widget("Sally"), cacheWithObjectKeys.get(widgetCacheKeyMaker.makeKey(new Widget("Sally"))));
        assertEquals(new Widget("Jane"), cacheWithObjectKeys.get(widgetCacheKeyMaker.makeKey(new Widget("Jane"))));
    }

    @Test
    public void testIndexCollectionWasUpdatedWithNullValues() {
        widgetStringCacheMaintainer.indexCollectionWasUpdated(null, null);
    }

    @Test
    public void testObjectWasAdded() {
        Widget value = new Widget("Bob");
        widgetStringCacheMaintainer.objectWasAdded(value);

        assertEquals(value, cacheWithObjectKeys.get(widgetCacheKeyMaker.makeKey(value)));
        assertEquals(1, cacheEntryCount());
    }

    @Test
    public void testSameObjectAddedIdempotency() {
        Widget value = new Widget("Bob");
        widgetStringCacheMaintainer.objectWasAdded(value);

        assertEquals(value, cacheWithObjectKeys.get(widgetCacheKeyMaker.makeKey(value)));
        assertEquals(value, cacheWithObjectKeys.get(widgetCacheKeyMaker.makeKey(value)));
        assertEquals(1, cacheEntryCount());
    }

    @Test
    public void testObjectWasRemoved() {
        Widget value = new Widget("Bob");
        widgetStringCacheMaintainer.objectWasAdded(value);
        widgetStringCacheMaintainer.objectWasRemoved(value);
        assertNull(cacheWithObjectKeys.get(widgetCacheKeyMaker.makeKey(value)));
        assertEquals(0, cacheEntryCount());
    }

    private int cacheEntryCount() {
        int count = 0;
        for (Object ignored : cacheWithObjectKeys) {
            count += 1;
        }
        return count;
    }
}

