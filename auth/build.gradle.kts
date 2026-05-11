plugins {
    kotlin("jvm")
}

object Versions {
    const val JWT = "0.12.6"
}

dependencies {
    implementation(project(":user"))
    implementation(project(":common"))

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    implementation("org.springframework.boot:spring-boot-starter-web")

    implementation("org.springframework.boot:spring-boot-starter-security")

    implementation("org.springframework.boot:spring-boot-starter-data-redis")

    implementation("io.jsonwebtoken:jjwt-api:${Versions.JWT}")

    runtimeOnly("io.jsonwebtoken:jjwt-impl:${Versions.JWT}")

    runtimeOnly("io.jsonwebtoken:jjwt-jackson:${Versions.JWT}")
}