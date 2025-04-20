FROM eclipse-temurin:21-jdk-alpine as builder

WORKDIR /user-service

COPY ./build/libs/*.jar user-service.jar


FROM eclipse-temurin:21-jre-alpine

WORKDIR /user-service

COPY --from=builder /user-service/user-service.jar user-service.jar

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "user-service.jar"]
