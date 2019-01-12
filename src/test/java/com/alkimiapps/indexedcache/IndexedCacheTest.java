/*
 * Copyright (c) 2019. Allan Boyd
 * This program is made available under the terms of the Apache License v2.0.
 */

package com.alkimiapps.indexedcache;

import com.alkimiapps.keys.Widget;
import com.googlecode.cqengine.ConcurrentIndexedCollection;
import com.googlecode.cqengine.IndexedCollection;
import com.googlecode.cqengine.attribute.Attribute;
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
import javax.cache.spi.CachingProvider;

import static com.googlecode.cqengine.query.QueryFactory.attribute;
import static com.googlecode.cqengine.query.QueryFactory.equal;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
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


//    private Widget testCreate() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
//        Class<com.alkimiapps.indexedcache.IndexedCacheTest.Widget> widgetClass = Class.forName("com.alkimiapps.indexedcache.IndexedCacheTest.Widget");
//        Widget instance = (Widget) Class.forName("com.alkimiapps.indexedcache.IndexedCacheTest.Widget").newInstance();
//        return instance;
//    }
}
