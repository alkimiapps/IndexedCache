/*
 * Copyright (c) 2019. Allan Boyd
 * This program is made available under the terms of the Apache License v2.0.
 */

package com.alkimiapps.keys;

public class Widget {
    private String name;

    public Widget(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Widget && ((Widget) o).name.equals(name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
