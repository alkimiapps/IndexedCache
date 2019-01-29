package com.alkimiapps.indexedcache.internal;

import com.alkimiapps.indexedcache.UniqueInstanceMaker;
import org.junit.Test;

import static org.junit.Assert.assertNotEquals;

public class SubclassableClassUniqueCackeKeyMakerTest {
    @Test
    public void testUniquenessWithPrivateAndPublicConstructorKeyClass() {
        testUniquenessWithKeyClass(PrivateAndPublicConstructor.class);
    }

    @Test
    public void testUniquenessWithDefaultConstructorKeyClass() {
        testUniquenessWithKeyClass(DefaultConstructor.class);
    }

    @Test
    public void testUniquenessWithPrivateAndProtectedConstructorKeyClass() {
        testUniquenessWithKeyClass(PrivateAndProtectedConstructor.class);
    }

    private <T> void testUniquenessWithKeyClass(Class<T> keyClass) {

        UniqueInstanceMaker<T> uniqueInstanceMaker = new SubclassableClassUniqueInstanceMaker<>();
        T uniqueCacheKey1 = uniqueInstanceMaker.makeUniqueInstance(keyClass);
        T uniqueCacheKey2 = uniqueInstanceMaker.makeUniqueInstance(keyClass);

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
