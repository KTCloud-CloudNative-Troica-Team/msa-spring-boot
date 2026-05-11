plugins {
    kotlin("jvm")
    kotlin("kapt")
    kotlin("plugin.jpa")
}

dependencies {
    implementation(project(":common"))

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    implementation("org.springframework.boot:spring-boot-starter-security")
}