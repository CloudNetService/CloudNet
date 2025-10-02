FROM azul/zulu-openjdk-debian:25-latest AS build

COPY . /home/cloudnet-build
WORKDIR /home/cloudnet-build

RUN chmod +x gradlew && ./gradlew -x test --no-daemon --stacktrace

FROM azul/zulu-openjdk-alpine:25-jre-headless-latest

RUN mkdir -p /cloudnet
WORKDIR /cloudnet
VOLUME /cloudnet

COPY --from=build /home/cloudnet-build/launcher/java22/build/libs/launcher.jar .
ENTRYPOINT exec java $JAVA_OPTS -jar launcher.jar $CLOUDNET_OPTS
