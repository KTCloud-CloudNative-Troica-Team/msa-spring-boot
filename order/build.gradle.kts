plugins {
    kotlin("jvm")
    kotlin("kapt")
    kotlin("plugin.jpa")
}

dependencies {
    implementation(project(":common"))

    implementation("com.github.kanei0415:ktcloud-msa-client-redis:v1.0.2")

    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core")

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    implementation("org.springframework.kafka:spring-kafka")

    runtimeOnly("com.h2database:h2")

    implementation("com.querydsl:querydsl-jpa:5.1.0:jakarta")
    kapt("com.querydsl:querydsl-apt:5.1.0:jakarta")
    kapt("jakarta.persistence:jakarta.persistence-api")
    kapt("jakarta.annotation:jakarta.annotation-api")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("net.datafaker:datafaker:2.4.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

sourceSets {
    main {
        kotlin.srcDir("build/generated/source/kapt/main")
    }
}