plugins {
    kotlin("jvm")
    kotlin("kapt")
    kotlin("plugin.jpa")
}

dependencies {
    implementation(project(":common"))

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    implementation("org.springframework.boot:spring-boot-starter-security")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("net.datafaker:datafaker:2.4.2")
}

tasks.withType<Test> {
    useJUnitPlatform()
}