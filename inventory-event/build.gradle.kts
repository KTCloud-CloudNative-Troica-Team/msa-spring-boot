plugins {
    kotlin("jvm")
    kotlin("kapt")
    kotlin("plugin.jpa")
}

dependencies {
    implementation(project(":common"))

    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    runtimeOnly("com.h2database:h2")

    implementation("com.querydsl:querydsl-jpa:5.1.0:jakarta")
    kapt("com.querydsl:querydsl-apt:5.1.0:jakarta")
    kapt("jakarta.persistence:jakarta.persistence-api")
    kapt("jakarta.annotation:jakarta.annotation-api")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("net.datafaker:datafaker:2.4.2")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

sourceSets {
    main {
        kotlin.srcDir("build/generated/source/kapt/main")
    }
}