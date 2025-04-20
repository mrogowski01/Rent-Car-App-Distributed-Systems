FROM eclipse-temurin:21-jdk-alpine as builder

WORKDIR /car-service

COPY ./build/libs/*.jar car-service.jar


FROM eclipse-temurin:21-jre-alpine

WORKDIR /car-service

COPY --from=builder /car-service/car-service.jar car-service.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "car-service.jar"]
