# syntax=docker/dockerfile:1

# Jar собирается отдельно, вне Dockerfile:
#   mvn -pl stats-parent/stats-service -am -Dmaven.test.skip=true package
# Здесь только упаковка готового артефакта.
#
# Контекст сборки — корень репозитория,
# поэтому путь к jar начинается с stats-parent/.

FROM amazoncorretto:21-alpine

RUN addgroup -S -g 1001 app \
    && adduser -S -u 1001 -G app -h /app -s /sbin/nologin app

WORKDIR /app

COPY --chown=app:app stats-parent/stats-service/target/stats-service-*.jar /app/app.jar

USER app

EXPOSE 9090

# /actuator/health открыт по умолчанию (spring-boot-starter-actuator, web exposure = health).
# busybox wget выходит с ненулевым кодом при 4xx/5xx, этого достаточно.
HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=6 \
    CMD wget -q -O /dev/null http://localhost:9090/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
