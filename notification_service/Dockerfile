FROM eclipse-temurin:21-jdk-alpine as builder

WORKDIR /notification_service

COPY ./build/libs/*.jar notification-service.jar


FROM eclipse-temurin:21-jre-alpine

WORKDIR /notification_service

COPY --from=builder /notification_service/notification-service.jar notification-service.jar

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "notification-service.jar"]
