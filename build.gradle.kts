plugins {
    java
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.allopen)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.micronaut.application)
    alias(libs.plugins.shadow)
    alias(libs.plugins.micronaut.test.resources)
    alias(libs.plugins.micronaut.aot)
}

version = "0.1"
group = "coralsum"

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform(libs.micronaut.platform))
    kapt(libs.micronaut.data.processor)
    kapt(libs.micronaut.http.validation)
    kapt(libs.micronaut.openapi)
    kapt(libs.micronaut.serde.processor)
    kapt(libs.micronaut.validation.processor)
    kapt(libs.micronaut.security.annotations)
    kapt(libs.jobrunr.micronaut.annotations)
    kapt(libs.mapstruct.processor)
    implementation(libs.weixin.java.mp)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.reactor)
    implementation(libs.reactor.kotlin.extensions)
    implementation(libs.okio)
    implementation(libs.google.genai)
    implementation(libs.langchain4j.google.ai.gemini)
    implementation(libs.langchain4j.http.client.jdk)
    implementation(libs.micronaut.security.jwt)
    implementation(libs.micronaut.security)
    implementation(libs.micrometer.context.propagation)
    implementation(libs.micronaut.aop)
    implementation(libs.micronaut.jackson.databind)
    implementation(libs.micronaut.retry)
    implementation(libs.micronaut.flyway)
    implementation(libs.micronaut.jdbc.hikari)
    implementation(libs.micronaut.cache.core)
    implementation(libs.micronaut.cache.caffeine)
    implementation(libs.micronaut.data.r2dbc)
    implementation(libs.micronaut.kotlin.ext.fns)
    implementation(libs.micronaut.kotlin.runtime)
    implementation(libs.micronaut.problem.json)
    implementation(libs.micronaut.objectstorage.aws)
    implementation(libs.micronaut.reactor)
    implementation(libs.micronaut.reactor.http.client)
    implementation(libs.micronaut.serde.jackson)
    implementation(libs.micronaut.validation)
    implementation(libs.micronaut.objectstorage.local)
    implementation(libs.resilience4j.consumer)
    implementation(libs.resilience4j.kotlin)
    implementation(libs.resilience4j.ratelimiter)
    implementation(libs.resilience4j.reactor)
    implementation(libs.resilience4j.micronaut)
    implementation(libs.jakarta.annotation.api)
    implementation(libs.jakarta.validation.api)
    implementation(libs.kotlin.reflect)
    implementation(libs.kotlin.stdlib.jdk8)
    implementation(libs.jobrunr.micronaut.feature)
    implementation(libs.hutool.all)
    implementation(libs.commons.lang3)
    implementation(libs.commons.collections4)
    implementation(libs.guava)
    implementation(libs.mapstruct)
    implementation(libs.h2)
    compileOnly(libs.micronaut.openapi.annotations)
    compileOnly(libs.jobrunr.micronaut.annotations)
    runtimeOnly(libs.r2dbc.h2)
    runtimeOnly(libs.snakeyaml)
    runtimeOnly(libs.logback.classic)
    runtimeOnly(libs.jackson.module.kotlin)
    testImplementation(libs.micronaut.http.client)
    testImplementation(libs.assertj.core)
    testRuntimeOnly(libs.junit.platform.launcher)
    testResourcesService(libs.postgresql)
}


application {
    mainClass = "coralsum.ApplicationKt"
}

java {
    sourceCompatibility = JavaVersion.toVersion("17")
}

graalvmNative.toolchainDetection = false

micronaut {
    runtime("netty")
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("coralsum.*")
    }
    aot {
        // Please review carefully the optimizations enabled below
        // Check https://micronaut-projects.github.io/micronaut-aot/latest/guide/ for more details
        optimizeServiceLoading = false
        convertYamlToJava = false
        precomputeOperations = true
        cacheEnvironment = true
        optimizeClassLoading = true
        deduceEnvironment = true
        optimizeNetty = true
        replaceLogbackXml = true
    }
}

allOpen {
    annotations("org.mapstruct.Mapper")
    annotations("jakarta.inject.Singleton")
    annotations("io.micronaut.http.annotation.Controller")
    annotations("iio.micronaut.validation.Validated")
}
