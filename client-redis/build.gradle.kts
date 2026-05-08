object Versions {
    const val REDISSON = "3.27.2"
    const val COROUTINE = "1.8.0"
}

plugins {
    kotlin("jvm")
    kotlin("kapt")

    `maven-publish`
}

dependencies {
    compileOnly("jakarta.servlet:jakarta.servlet-api:6.0.0")

    api("org.springframework.boot:spring-boot-starter-data-redis")

    implementation("org.redisson:redisson-spring-boot-starter:${Versions.REDISSON}")

    implementation("org.springframework.boot:spring-boot-starter-aop")

    kapt("org.springframework.boot:spring-boot-configuration-processor")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.COROUTINE}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor:${Versions.COROUTINE}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:${Versions.COROUTINE}")
}

publishing {
    publications {
        create<MavenPublication>("gpr") {
            from(components["java"])
            groupId = "com.kanei0415"
            artifactId = "ktcloud-market-msa-client-redis"
            version = "1.0.0"
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/kanei0415/ktcloud-market-msa-client-redis")
            credentials {
                username = "kanei0415"
                password = System.getenv("GPR_TOKEN")
            }
        }
    }
}