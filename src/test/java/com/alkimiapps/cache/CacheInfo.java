package com.alkimiapps.cache;

import javax.cache.Cache;

public final class CacheInfo {
    public static int cacheEntryCount(Cache cache) {
        int count = 0;
        for (Object ignored : cache) {
            count += 1;
        }
        return count;
    }
}
