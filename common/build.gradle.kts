plugins {
    kotlin("jvm")
    kotlin("kapt")
    kotlin("plugin.jpa")

    id("java-library")
    id("maven-publish")
}

group = "com.github.kanei0415"
version = "1.0.0"

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    implementation("org.springframework.boot:spring-boot-starter-web")

    runtimeOnly("com.h2database:h2")

    implementation("com.querydsl:querydsl-jpa:5.1.0:jakarta")
    kapt("com.querydsl:querydsl-apt:5.1.0:jakarta")
    kapt("jakarta.persistence:jakarta.persistence-api")
    kapt("jakarta.annotation:jakarta.annotation-api")
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