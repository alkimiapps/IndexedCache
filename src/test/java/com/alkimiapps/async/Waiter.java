/*
 * Copyright (c) 2019. Allan Boyd
 * This program is made available under the terms of the Apache License v2.0.
 */

package com.alkimiapps.async;

import java.util.Date;
import java.util.function.Supplier;

public final class Waiter {

    private static final int DEFAULT_TIMEOUT_MILLIS = 2000;

    public static <T> void waitForValueWithTimeout(Supplier<T> supplier) {
        Waiter.waitForValueWithTimeout(supplier, DEFAULT_TIMEOUT_MILLIS);
    }

    private static <T> void waitForValueWithTimeout(Supplier<T> supplier, int timeoutMillis) {
        Long start = new Date().getTime();
        while(supplier.get() == null && new Date().getTime() - start < timeoutMillis) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                // Don't care
            }
        }
    }

    public static void justWaitMillis(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // Don't care
        }
    }
}
