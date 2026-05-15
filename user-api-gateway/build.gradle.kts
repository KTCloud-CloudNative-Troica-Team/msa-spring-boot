object Versions {
    const val GRPC = "4.34.1"
    const val GRPC_KOTLIN = "1.4.1"
    const val GRPC_PROTO = "1.80.0"
    const val JWT = "0.12.6"
    const val BUCKET4J = "8.14.0"
}

plugins {
    kotlin("jvm")
    id("com.google.protobuf") version "0.10.0"
}

dependencies {
    implementation(project(":common"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-reactor")

    implementation("org.springframework.boot:spring-boot-starter-actuator")

    implementation("net.devh:grpc-client-spring-boot-starter:3.1.0.RELEASE")

    implementation("com.google.protobuf:protobuf-kotlin:${Versions.GRPC}")
    implementation("io.grpc:grpc-kotlin-stub:${Versions.GRPC_KOTLIN}")

    implementation("io.grpc:grpc-protobuf:${Versions.GRPC_PROTO}")
    implementation("io.grpc:grpc-netty:${Versions.GRPC_PROTO}")
    implementation("io.grpc:grpc-stub:${Versions.GRPC_PROTO}")
    implementation("io.grpc:grpc-netty-shaded:${Versions.GRPC_PROTO}")

    implementation("org.springframework.cloud:spring-cloud-starter-gateway:4.1.9")
    implementation("org.springframework.boot:spring-boot-starter-webflux:3.3.13")
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive:3.3.13")

    implementation("com.bucket4j:bucket4j_jdk17-core:${Versions.BUCKET4J}")
    implementation("com.bucket4j:bucket4j_jdk17-lettuce:${Versions.BUCKET4J}")
    implementation("org.springframework.boot:spring-boot-starter-aop:3.3.13")
    implementation("io.github.resilience4j:resilience4j-spring-boot3:2.2.0")
    implementation("io.github.resilience4j:resilience4j-reactor:2.2.0")
    implementation("io.github.resilience4j:resilience4j-kotlin:2.2.0")

    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.5.0")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    implementation("org.springframework.boot:spring-boot-starter-security")

    implementation("io.jsonwebtoken:jjwt-api:${Versions.JWT}")

    runtimeOnly("io.jsonwebtoken:jjwt-impl:${Versions.JWT}")

    runtimeOnly("io.jsonwebtoken:jjwt-jackson:${Versions.JWT}")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("io.mockk:mockk-agent-jvm:1.13.13")
    testImplementation("net.datafaker:datafaker:2.4.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test")
    testImplementation("io.projectreactor:reactor-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

sourceSets{
    getByName("main"){
        java {
            srcDirs(
                "build/generated/source/proto/main/java",
                "build/generated/source/proto/main/kotlin"
            )
        }
    }
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${Versions.GRPC}"
    }

    plugins {
        register("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:${Versions.GRPC_PROTO}"
        }
        register("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:${Versions.GRPC_KOTLIN}:jdk8@jar"
        }
    }
    generateProtoTasks {
        all().forEach {
            it.plugins {
                register("grpc")
                register("grpckt")
            }
            it.builtins {
                register("kotlin")
            }
        }
    }
}