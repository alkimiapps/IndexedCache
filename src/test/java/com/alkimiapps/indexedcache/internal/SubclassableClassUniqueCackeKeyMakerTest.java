package com.alkimiapps.indexedcache.internal;

import com.alkimiapps.indexedcache.UniqueCacheKeyMaker;
import org.junit.Test;

import static org.junit.Assert.assertNotEquals;

public class SubclassableClassUniqueCackeKeyMakerTest {
    @Test
    public void testUniquenessWithPrivateAndPublicConstructorKeyClass() {
        UniqueCacheKeyMaker<PrivateAndPublicConstructor> uniqueCacheKeyMaker = new SubclassableClassUniqueCacheKeyMaker<>();
        PrivateAndPublicConstructor uniqueCacheKey1 = uniqueCacheKeyMaker.makeUniqueCacheKeyForCache(PrivateAndPublicConstructor.class);
        PrivateAndPublicConstructor uniqueCacheKey2 = uniqueCacheKeyMaker.makeUniqueCacheKeyForCache(PrivateAndPublicConstructor.class);

        assertNotEquals(uniqueCacheKey1, uniqueCacheKey2);
        assertNotEquals(0, uniqueCacheKey1.hashCode());
        assertNotEquals(0, uniqueCacheKey2.hashCode());
        assertNotEquals(uniqueCacheKey1.hashCode(), uniqueCacheKey2.hashCode());

    }

    @Test
    public void testUniquenessWithDefaultConstructorKeyClass() {
        UniqueCacheKeyMaker<DefaultConstructor> uniqueCacheKeyMaker = new SubclassableClassUniqueCacheKeyMaker<>();
        DefaultConstructor uniqueCacheKey1 = uniqueCacheKeyMaker.makeUniqueCacheKeyForCache(DefaultConstructor.class);
        DefaultConstructor uniqueCacheKey2 = uniqueCacheKeyMaker.makeUniqueCacheKeyForCache(DefaultConstructor.class);

        assertNotEquals(uniqueCacheKey1, uniqueCacheKey2);
        assertNotEquals(0, uniqueCacheKey1.hashCode());
        assertNotEquals(0, uniqueCacheKey2.hashCode());
        assertNotEquals(uniqueCacheKey1.hashCode(), uniqueCacheKey2.hashCode());

    }

    @Test
    public void testUniquenessWithPrivateAndProtectedConstructorKeyClass() {
        UniqueCacheKeyMaker<PrivateAndProtectedConstructor> uniqueCacheKeyMaker = new SubclassableClassUniqueCacheKeyMaker<>();
        PrivateAndProtectedConstructor uniqueCacheKey1 = uniqueCacheKeyMaker.makeUniqueCacheKeyForCache(PrivateAndProtectedConstructor.class);
        PrivateAndProtectedConstructor uniqueCacheKey2 = uniqueCacheKeyMaker.makeUniqueCacheKeyForCache(PrivateAndProtectedConstructor.class);

        assertNotEquals(uniqueCacheKey1, uniqueCacheKey2);
        assertNotEquals(0, uniqueCacheKey1.hashCode());
        assertNotEquals(0, uniqueCacheKey2.hashCode());
        assertNotEquals(uniqueCacheKey1.hashCode(), uniqueCacheKey2.hashCode());

    }

    public class PrivateAndPublicConstructor {

        private PrivateAndPublicConstructor() {}

        public PrivateAndPublicConstructor(String name) {}

        public boolean equals(Object o) {
            return true;
        }

        public int hashCode() {
            return 0;
        }
    }


    public class PrivateAndProtectedConstructor {

        private PrivateAndProtectedConstructor() {}

        protected PrivateAndProtectedConstructor(String name) {}

        public boolean equals(Object o) {
            return true;
        }

        public int hashCode() {
            return 0;
        }
    }

    public class DefaultConstructor {

        public boolean equals(Object o) {
            return true;
        }

        public int hashCode() {
            return 0;
        }
    }
}
