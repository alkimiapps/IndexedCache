/*
 * Copyright (c) 2019. Allan Boyd
 * This program is made available under the terms of the Apache License v2.0.
 */

package com.alkimiapps.indexedcache.internal;

import com.alkimiapps.indexedcache.UniqueInstanceMaker;
import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import org.objenesis.ObjenesisHelper;

import java.lang.reflect.Method;
import java.util.UUID;

/**
 * A UniqueInstanceMaker that can make unique instances for classes that are non-final and that have at
 * least one public or protected constructor.
 */
public final class SubclassableClassUniqueInstanceMaker<K> implements UniqueInstanceMaker<K> {
    public K makeUniqueInstance(Class<K> instanceType) {
        MethodInterceptor methodInterceptor = new UniqueMethodInterceptor();
        Enhancer enhancer = new Enhancer();
        enhancer.setSuperclass(instanceType);
        enhancer.setUseCache(false);

        enhancer.setCallbackType(methodInterceptor.getClass());
        Class<K> proxyClass = enhancer.createClass();
        Enhancer.registerCallbacks(proxyClass, new Callback[]{methodInterceptor});
        return ObjenesisHelper.newInstance(proxyClass);
    }

    private class UniqueMethodInterceptor implements MethodInterceptor {

        @Override
        public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {
            if (isHashCodeMethod(method)) {
                return UUID.randomUUID().hashCode();
            }
            if (isEqualsMethod(method)) {
                return false;
            }
            if (isCompareToMethod(method)) {
                return 1;
            }

            return proxy.invokeSuper(obj, args);
        }

        private boolean isHashCodeMethod(Method method) {
            return method.getName().equals("hashCode") && method.getReturnType() == int.class && method.getParameterCount() == 0;
        }

        private boolean isEqualsMethod(Method method) {
            return method.getName().equals("equals") && method.getReturnType() == boolean.class && method.getParameterCount() == 1 && method.getParameterTypes()[0].equals(Object.class);
        }

        private boolean isCompareToMethod(Method method) {
            return method.getName().equals("compareTo") && method.getReturnType() == int.class && method.getParameterCount() == 1;
        }
    }
}
