package com.alkimiapps.indexedcache;

import com.alkimiapps.async.Waiter;
import com.alkimiapps.keys.Widget;
import com.googlecode.cqengine.ConcurrentIndexedCollection;
import com.googlecode.cqengine.IndexedCollection;
import com.googlecode.cqengine.attribute.Attribute;
import com.googlecode.cqengine.index.radixreversed.ReversedRadixTreeIndex;
import com.googlecode.cqengine.query.Query;
import com.googlecode.cqengine.resultset.ResultSet;
import org.ehcache.config.CacheConfiguration;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheEventListenerConfigurationBuilder;
import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.event.CacheEventListener;
import org.ehcache.event.EventType;
import org.ehcache.jsr107.Eh107Configuration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;

import java.time.Duration;
import java.util.UUID;

import static com.googlecode.cqengine.query.QueryFactory.attribute;
import static com.googlecode.cqengine.query.QueryFactory.equal;
import static org.junit.Assert.*;

/**
 * This test class is for verifying the behaviour of the IndexedCache when it is associated with a
 * JCache that has limitations on its capacity and ttl. In addition the behaviour of IndexedCache when
 * entries are evicted from the associated JCache is covered in this test.
 */
public class LimitedCacheIndexedCacheTest {

    private static final long CACHE_TTL_MILLIS = 50L;
    private IndexedCache<Widget, Widget> indexedCache;
    private Cache<Widget, Widget> cache;
    private CacheKeyMaker<Widget, Widget> cacheKeyMaker = new IdentityCacheKeyMaker<>();
    private static final Attribute<Widget, String> Widget_Name = attribute("widgetName", Widget::getName);

    @Before
    public void setup() {
        // Unfortunately jsr107 doesn't provide an event for cache eviction and remove events are generally only
        // supported for cache.remove() calls (see https://github.com/jsr107/jsr107spec/issues/403). That means
        // we do not get notified of eviction in the JSR107 cache that is associated with the IndexedCache. In
        // order to keep the IndexedCache in sync with the cache, we need to observe eviction externally and
        // remove evicted items ourselves - the IndexedCache cannot do this for us (at least not in a sane manner).
        CacheEventListener<Widget, Widget> listener = event -> indexedCache.remove(event.getOldValue());

        CacheEventListenerConfigurationBuilder cacheEventListenerConfiguration = CacheEventListenerConfigurationBuilder
                .newEventListenerConfiguration(listener, EventType.EVICTED)
                .unordered().asynchronous();
        CachingProvider provider = Caching.getCachingProvider();
        CacheManager cacheManager = provider.getCacheManager();

        // Configure a cache with a maximum of 1 in-memory entry
        CacheConfiguration<Widget, Widget> cacheConfiguration = CacheConfigurationBuilder.newCacheConfigurationBuilder(Widget.class, Widget.class,
                ResourcePoolsBuilder.heap(1).build())
                .add(cacheEventListenerConfiguration)
                .build();

        cache = cacheManager.createCache(UUID.randomUUID().toString(),
                Eh107Configuration.fromEhcacheCacheConfiguration(cacheConfiguration));

        IndexedCollection<Widget> indexedCollection = new ConcurrentIndexedCollection<>();
        indexedCache = new IndexedCache<>(indexedCollection, cache, cacheKeyMaker);
        indexedCache.addIndex(ReversedRadixTreeIndex.onAttribute(Widget_Name));

    }

    @After
    public void tearDown() {
        if (cache != null && !cache.isClosed()) {
            cache.close();
        }
    }

    @Test
    public void testRemoveFromCacheRemovesFromIndexedCache() {

        Widget frank = new Widget("Frank");
        assertTrue(indexedCache.add(frank));
        assertTrue(cache.remove(frank));

        // Cache entry observers maybe informed of changes on a background thread, so we need to bear that in mind
        Waiter.waitForValueWithTimeout(() -> indexedCache.contains(frank) ? null : frank);
        Query<Widget> queryFrank = equal(Widget_Name, "Frank");
        ResultSet<Widget> results = indexedCache.retrieve(queryFrank);
        assertEquals(0, results.size());
        assertFalse(indexedCache.contains(frank));
    }

    @Test
    public void testExceedingMaxEntriesRemovesOlderFromCacheAndFromIndexedCache() {
        Widget frank = new Widget("Frank");
        assertTrue(indexedCache.add(frank));

        Query<Widget> queryFrank = equal(Widget_Name, "Frank");
        ResultSet<Widget> results = indexedCache.retrieve(queryFrank);
        assertEquals(1, results.size());
        assertTrue(cache.containsKey(frank));

        Widget bob = new Widget("Bob");
        assertTrue(indexedCache.add(bob));

        // Check that frank has been replaced by bob in the underlying cache
        assertTrue(cache.containsKey(bob));
        assertFalse(cache.containsKey(frank));

        // That replacement of frank by bob has evicted frank which should have been observed and resulted in
        // frank being evicted from the indexed cache too....

        // Cache entry observers maybe informed of changes on a background thread, so we need to bear that in mind
        Waiter.waitForValueWithTimeout(() -> indexedCache.contains(frank) ? null : frank);
        results = indexedCache.retrieve(queryFrank);
        assertEquals(0, results.size());
    }

    @Test
    public void testExpiredCacheEntriesAreRemovedFromTheIndexedCache() {

        // Reconfigure the cache with an expiry.

        CachingProvider provider = Caching.getCachingProvider();
        CacheManager cacheManager = provider.getCacheManager();

        // Configure a cache with a maximum of 1 in-memory entry
        CacheConfiguration<Widget, Widget> cacheConfiguration = CacheConfigurationBuilder.newCacheConfigurationBuilder(Widget.class, Widget.class,
                ResourcePoolsBuilder.heap(1).build())
                .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofMillis(CACHE_TTL_MILLIS)))
                .build();

        cache = cacheManager.createCache("jCacheWithExpiry",
                Eh107Configuration.fromEhcacheCacheConfiguration(cacheConfiguration));

        IndexedCollection<Widget> indexedCollection = new ConcurrentIndexedCollection<>();
        indexedCache = new IndexedCache<>(indexedCollection, cache, cacheKeyMaker);
        indexedCache.addIndex(ReversedRadixTreeIndex.onAttribute(Widget_Name));

        Widget frank = new Widget("Frank");
        assertTrue(indexedCache.add(frank));

        Query<Widget> queryFrank = equal(Widget_Name, "Frank");
        ResultSet<Widget> results = indexedCache.retrieve(queryFrank);
        assertEquals(1, results.size());

        Waiter.justWaitMillis(Math.toIntExact(CACHE_TTL_MILLIS + 1));

        assertNull(cache.get(frank)); // Getting frank after he has expired causes an expiry event

        Waiter.waitForValueWithTimeout(() -> indexedCache.contains(frank) ? null : frank);
        results = indexedCache.retrieve(queryFrank);
        assertEquals(0, results.size());
    }
}
