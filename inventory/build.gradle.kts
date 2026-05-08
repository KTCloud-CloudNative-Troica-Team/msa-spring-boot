plugins {
    kotlin("jvm")
    kotlin("plugin.jpa")
}

dependencies {
    implementation("com.github.kanei0415:ktcloud-msa-common:v1.0.0")
    implementation("com.github.kanei0415:ktcloud-msa-client-redis:v1.0.0")
    implementation(project(":inventory-event"))

    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    implementation("org.springframework.kafka:spring-kafka")

    runtimeOnly("com.h2database:h2")
}