FROM azul/zulu-openjdk-alpine:17-jre AS build

COPY . /home/cloudnet-build
WORKDIR /home/cloudnet-build
VOLUME /root/.gradle

RUN chmod +x gradlew && ./gradlew -x test --no-daemon --stacktrace

FROM azul/zulu-openjdk-alpine:17-jre

RUN mkdir -p /cloudnet
WORKDIR /cloudnet
VOLUME /cloudnet

COPY --from=build /home/cloudnet-build/launcher/java17/build/libs/launcher.jar .
ENTRYPOINT exec java $JAVA_OPTS -jar launcher.jar $CLOUDNET_OPTS
