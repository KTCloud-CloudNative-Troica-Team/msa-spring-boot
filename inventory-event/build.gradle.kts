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
}

sourceSets {
    main {
        kotlin.srcDir("build/generated/source/kapt/main")
    }
}