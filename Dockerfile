# Multi-stage Dockerfile shared by every bootable service.
#
# Build:
#   docker build --build-arg SERVICE=order-service -t market/order-service .
# Run:
#   docker run --env-file .env -p 8002:8002 market/order-service

# -------- builder --------
FROM eclipse-temurin:21-jdk AS builder
ARG SERVICE
WORKDIR /workspace

# Cache gradle wrapper + lockfiles first
COPY gradlew .
COPY gradle gradle
COPY settings.gradle.kts build.gradle.kts ./

# Copy all module build files (gradle needs them to compute the project graph)
COPY common/build.gradle.kts common/build.gradle.kts
COPY client-redis/build.gradle.kts client-redis/build.gradle.kts
COPY inventory/build.gradle.kts inventory/build.gradle.kts
COPY inventory-event/build.gradle.kts inventory-event/build.gradle.kts
COPY order/build.gradle.kts order/build.gradle.kts
COPY product/build.gradle.kts product/build.gradle.kts
COPY user/build.gradle.kts user/build.gradle.kts
COPY auth/build.gradle.kts auth/build.gradle.kts
COPY user-api-gateway/build.gradle.kts user-api-gateway/build.gradle.kts
COPY admin-api-gateway/build.gradle.kts admin-api-gateway/build.gradle.kts
COPY product-service/build.gradle.kts product-service/build.gradle.kts
COPY order-service/build.gradle.kts order-service/build.gradle.kts
COPY inventory-service/build.gradle.kts inventory-service/build.gradle.kts
COPY auth-service/build.gradle.kts auth-service/build.gradle.kts

# Now copy sources
COPY common common
COPY client-redis client-redis
COPY inventory inventory
COPY inventory-event inventory-event
COPY order order
COPY product product
COPY user user
COPY auth auth
COPY user-api-gateway user-api-gateway
COPY admin-api-gateway admin-api-gateway
COPY product-service product-service
COPY order-service order-service
COPY inventory-service inventory-service
COPY auth-service auth-service

RUN chmod +x gradlew && ./gradlew :${SERVICE}:bootJar --no-daemon -x test
RUN cp ${SERVICE}/build/libs/*.jar /workspace/app.jar

# -------- runtime --------
FROM eclipse-temurin:21-jre AS runtime
ARG SERVICE
ENV SERVICE_NAME=${SERVICE}

WORKDIR /app
COPY --from=builder /workspace/app.jar /app/app.jar

RUN addgroup --system app && adduser --system --ingroup app app && chown -R app:app /app
USER app

ENV JAVA_OPTS=""
ENV SPRING_PROFILES_ACTIVE=""
EXPOSE 8000 9000

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
