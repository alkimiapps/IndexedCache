/*
 * Copyright (c) 2019. Allan Boyd
 * This program is made available under the terms of the Apache License v2.0.
 */

package com.alkimiapps.indexedcache.internal;

import com.alkimiapps.indexedcache.UniqueCacheKeyMaker;
import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.objenesis.ObjenesisHelper;

import javax.cache.Cache;
import javax.cache.configuration.Configuration;
import java.lang.reflect.Method;
import java.util.UUID;

/**
 * A UniqueCacheKeyMaker that can make unique cache keys for cache key classes that are non-final and that have at
 * least one public or protected constructor.
 */
public final class SubclassableClassUniqueCacheKeyMaker<K> implements UniqueCacheKeyMaker<K> {
    public K makeUniqueCacheKeyForCache(Class<K> keyType) {
        MethodInterceptor methodInterceptor = new UniqueMethodInterceptor();
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(keyType);
        enhancer.setUseCache(false);

        enhancer.setCallbackType(methodInterceptor.getClass());
        Class<K> proxyClass = enhancer.createClass();
        Enhancer.registerCallbacks(proxyClass, new Callback[]{methodInterceptor});
        return ObjenesisHelper.newInstance(proxyClass);
    }

    private class UniqueMethodInterceptor implements MethodInterceptor {

        @Override
        public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
            if (method.getName().equals("hashCode") && method.getReturnType() == int.class && method.getParameterCount() == 0) {
                return UUID.randomUUID().hashCode();
            }
            if (method.getName().equals("equals") && method.getReturnType() == boolean.class && method.getParameterCount() == 1 && method.getParameterTypes()[0].equals(Object.class)) {
                return false;
            }
            if (method.getName().equals("compareTo") && method.getReturnType() == int.class && method.getParameterCount() == 1) {
                return 1;
            }

            return proxy.invokeSuper(obj, args);
        }
    }
}
