FROM --platform=$BUILDPLATFORM azul/zulu-openjdk:23 AS builder
WORKDIR /build

COPY ./ ./
RUN chmod +x gradlew && ./gradlew -x test -x checkstyleMain -x checkstyleTest --no-daemon --stacktrace

FROM azul/zulu-openjdk-alpine:23-jre-headless
WORKDIR /cloudnet

COPY --from=builder /build/launcher/java22/build/libs/launcher.jar .
ENTRYPOINT ["java", "-Xms128M", "-Xmx128M", "-XX:+PerfDisableSharedMem", "-jar", "launcher.jar"]
