plugins {
    java
    id("org.jetbrains.kotlin.jvm") version "2.0.20"
    id("org.jetbrains.kotlin.plugin.allopen") version "2.0.20"
    id("org.jetbrains.kotlin.kapt") version "2.0.20"
//    id("com.google.devtools.ksp") version "2.0.20-1.0.24"
    id("io.micronaut.application") version "4.6.1"
    id("com.gradleup.shadow") version "8.3.9"
    id("io.micronaut.test-resources") version "4.6.1"
    id("io.micronaut.aot") version "4.6.1"
}

version = "0.1"
group = "coralsum"

val kotlinVersion = project.properties["kotlinVersion"]

repositories {
    mavenCentral()
}

dependencies {
    kapt("io.micronaut.data:micronaut-data-processor")
    kapt("io.micronaut:micronaut-http-validation")
//    kapt("io.micronaut.micrometer:micronaut-micrometer-annotation")
    kapt("io.micronaut.openapi:micronaut-openapi")
    kapt("io.micronaut.serde:micronaut-serde-processor")
    kapt("io.micronaut.validation:micronaut-validation-processor")
    kapt("io.micronaut.security:micronaut-security-annotations")
    kapt("org.mapstruct:mapstruct-processor:1.5.5.Final")
    implementation("com.github.binarywang:weixin-java-mp:4.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")
    implementation("io.projectreactor.kotlin:reactor-kotlin-extensions")
    implementation("com.squareup.okio:okio:3.9.1")
    implementation("com.google.genai:google-genai:1.25.0")
    implementation("io.micronaut.security:micronaut-security")
//    implementation("io.micronaut.reactor:micronaut-security-reactor")
    implementation("io.micrometer:context-propagation")
    implementation("io.micronaut:micronaut-aop")
    implementation("io.micronaut:micronaut-jackson-databind")
//    implementation("io.micronaut:micronaut-management")
    implementation("io.micronaut:micronaut-retry")
    implementation("io.micronaut.flyway:micronaut-flyway")
    implementation("io.micronaut.sql:micronaut-jdbc-hikari")
//    implementation("io.micronaut.cache:micronaut-cache-caffeine")
    implementation("io.micronaut.data:micronaut-data-r2dbc")
//    implementation("io.micronaut.jmx:micronaut-jmx")
    implementation("io.micronaut.kotlin:micronaut-kotlin-extension-functions")
    implementation("io.micronaut.kotlin:micronaut-kotlin-runtime")
//    implementation("io.micronaut.micrometer:micronaut-micrometer-core")
//    implementation("io.micronaut.netflix:micronaut-netflix-hystrix")
    implementation("io.micronaut.objectstorage:micronaut-object-storage-aws")
    implementation("io.micronaut.reactor:micronaut-reactor")
    implementation("io.micronaut.reactor:micronaut-reactor-http-client")
    implementation("io.micronaut.serde:micronaut-serde-jackson")
    runtimeOnly("org.yaml:snakeyaml")
    implementation("io.micronaut.validation:micronaut-validation")
    implementation("io.micronaut.objectstorage:micronaut-object-storage-local")
    implementation("jakarta.annotation:jakarta.annotation-api")
    implementation("jakarta.validation:jakarta.validation-api")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${kotlinVersion}")
//    implementation("org.jobrunr:jobrunr-micronaut-feature:6.3.5")
    implementation("cn.hutool:hutool-all:5.8.40")
    implementation("org.apache.commons:commons-lang3:3.18.0")
    implementation("org.apache.commons:commons-collections4:4.4")
    implementation("com.google.guava:guava:33.0.0-jre")
    implementation("org.mapstruct:mapstruct:1.5.5.Final")
    implementation("com.h2database:h2")
    compileOnly("io.micronaut.openapi:micronaut-openapi-annotations")
    runtimeOnly("io.r2dbc:r2dbc-h2")
    runtimeOnly("ch.qos.logback:logback-classic")
    runtimeOnly("com.fasterxml.jackson.module:jackson-module-kotlin")
    testImplementation("io.micronaut:micronaut-http-client")
    testImplementation("org.assertj:assertj-core")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testResourcesService("org.postgresql:postgresql")
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
