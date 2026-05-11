pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "market"

include(
    "inventory",
    "inventory-service",
    "order",
    "order-service",
    "product",
    "product-service",
    "common",
    "client-redis",
    "inventory-event",
    "user",
    "auth",
    "user-api-gateway",
    "auth-service",
)