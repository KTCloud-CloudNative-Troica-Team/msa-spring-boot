plugins {
    kotlin("jvm")
    kotlin("plugin.jpa")
}

dependencies {
    implementation("https://maven.pkg.github.com/kanei0415/ktcloud-market-msa-common:1.0.0")
    implementation("https://maven.pkg.github.com/kanei0415/ktcloud-market-msa-client-redis:1.0.0")
    implementation(project(":inventory-event"))

    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    implementation("org.springframework.kafka:spring-kafka")

    runtimeOnly("com.h2database:h2")
}