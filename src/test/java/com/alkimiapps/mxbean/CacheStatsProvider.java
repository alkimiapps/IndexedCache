/*
 * Copyright (c) 2019. Allan Boyd
 * This program is made available under the terms of the Apache License v2.0.
 */

package com.alkimiapps.mxbean;

import javax.cache.management.CacheStatisticsMXBean;
import javax.management.JMX;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.lang.management.ManagementFactory;
import java.util.Set;

public final class CacheStatsProvider {
    public static CacheStatisticsMXBean getCacheStatisticsMXBean(final String cacheName) {
        final MBeanServer mbeanServer = ManagementFactory.getPlatformMBeanServer();
        ObjectName name;
        try {
            name = new ObjectName("*:type=CacheStatistics,*,Cache=" + cacheName);
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
        Set<ObjectName> beans = mbeanServer.queryNames(name, null);
        if (beans.isEmpty()) {
            return null;
        }
        ObjectName[] objArray = beans.toArray(new ObjectName[beans.size()]);
        return JMX.newMBeanProxy(mbeanServer, objArray[0], CacheStatisticsMXBean.class);
    }
}
