/*
 * Copyright (c) 2019. Allan Boyd
 * This program is made available under the terms of the Apache License v2.0.
 */

package com.alkimiapps.indexedcache;

/**
 * A thing that makes unique instances of some type.
 */
public interface UniqueInstanceMaker<T> {
    /**
     * The result of invoking equals on the returned T must never resolve to true and the
     * hashCode of the returned T is expected to be at least as unique as Object.hashCode().
     */
    T makeUniqueInstance(Class<T> instanceType);
}
