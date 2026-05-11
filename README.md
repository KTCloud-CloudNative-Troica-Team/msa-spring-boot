### Ready to Run

- gRPC ProtoBuff IDL 생성
```terminaloutput
➜  msa-spring-boot git:(main) ./gradlew generateProto
```

- QueryDSL kapt QClass 생성
```terminaloutput
➜  msa-spring-boot git:(main) ✗ ./gradlew kaptKotlin
```

- Local Middleware Dependency
```terminaloutput
➜  msa-spring-boot git:(main) ✗ docker-compose up -d -f container-compose.yaml
```

- order-service 시작
```terminaloutput
➜  msa-spring-boot git:(main) ✗ ./gradlew :order-service:bootRun
```

- product-service 시작
```terminaloutput
➜  msa-spring-boot git:(main) ✗ ./gradlew :product-service:bootRun
```

- inventory-service 시작
```terminaloutput
➜  msa-spring-boot git:(main) ✗ ./gradlew :inventory-service:bootRun
```

- SwaggerUI
http://localhost:8100/webjars/swagger-ui/index.html