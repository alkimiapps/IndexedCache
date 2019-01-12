/*
 * Copyright (c) 2019. Allan Boyd
 * This program is made available under the terms of the Apache License v2.0.
 */

group = "com.alkimiapps"
version = "0.1"

apply {
    plugin("java")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("javax.cache", "cache-api", "1.1.0")
    implementation("com.googlecode.cqengine", "cqengine", "3.0.0")
    implementation("cglib", "cglib", "3.2.10")
    implementation("org.objenesis", "objenesis", "3.0.1")

    testCompile("org.ehcache", "ehcache", "3.6.1")
    testCompile("junit", "junit", "4.12")
    testCompile("org.mockito", "mockito-core", "2.23.4")
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
}
