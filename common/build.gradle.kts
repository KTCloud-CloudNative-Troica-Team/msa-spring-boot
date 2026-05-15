plugins {
    kotlin("jvm")
    kotlin("kapt")
    kotlin("plugin.jpa")

    id("java-library")
    id("maven-publish")
}

group = "com.github.kanei0415"
version = "1.0.0"

object Versions {
    const val GRPC_PROTO = "1.80.0"
    const val GRPC_DEVH = "3.1.0.RELEASE"
    const val BUCKET4J = "8.14.0"
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    compileOnly("org.springframework.boot:spring-boot-starter-web")

    api("io.micrometer:micrometer-tracing-bridge-otel:1.3.5")
    api("io.opentelemetry:opentelemetry-exporter-otlp:1.43.0")
    api("net.logstash.logback:logstash-logback-encoder:8.0")

    compileOnly("io.grpc:grpc-api:${Versions.GRPC_PROTO}")
    compileOnly("io.grpc:grpc-stub:${Versions.GRPC_PROTO}")
    compileOnly("net.devh:grpc-server-spring-boot-starter:${Versions.GRPC_DEVH}")

    compileOnly("com.bucket4j:bucket4j_jdk17-core:${Versions.BUCKET4J}")
    compileOnly("com.bucket4j:bucket4j_jdk17-lettuce:${Versions.BUCKET4J}")
    compileOnly("io.lettuce:lettuce-core")
    compileOnly("org.springframework.boot:spring-boot-starter-data-redis")
    compileOnly("io.projectreactor:reactor-core")

    runtimeOnly("com.h2database:h2")

    implementation("com.querydsl:querydsl-jpa:5.1.0:jakarta")
    kapt("com.querydsl:querydsl-apt:5.1.0:jakarta")
    kapt("jakarta.persistence:jakarta.persistence-api")
    kapt("jakarta.annotation:jakarta.annotation-api")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("net.datafaker:datafaker:2.4.2")
    testImplementation("io.grpc:grpc-api:${Versions.GRPC_PROTO}")
    testImplementation("io.grpc:grpc-stub:${Versions.GRPC_PROTO}")
    testImplementation("com.bucket4j:bucket4j_jdk17-core:${Versions.BUCKET4J}")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

sourceSets {
    main {
        kotlin.srcDir("build/generated/source/kapt/main")
    }
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "ktcloud-market-msa-common"
            from(components["java"])
        }
    }
}

tasks.jar {
    enabled = true
    archiveClassifier.set("")
}

tasks.getByName<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    enabled = false
}