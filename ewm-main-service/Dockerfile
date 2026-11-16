# syntax=docker/dockerfile:1

FROM amazoncorretto:21-alpine

RUN addgroup -S -g 1001 app \
    && adduser -S -u 1001 -G app -h /app -s /sbin/nologin app

WORKDIR /app

COPY --chown=app:app ewm-main-service/target/ewm-main-service-*.jar /app/app.jar

USER app

EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=6 \
    CMD wget -q -O /dev/null http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
