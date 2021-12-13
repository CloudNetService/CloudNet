FROM gradle:7.3.1-jdk17 AS build
USER root

COPY . /usr/src/cloudnet-sources
WORKDIR /usr/src/cloudnet-sources

RUN gradle

FROM openjdk:17.0.1-slim
USER root

RUN mkdir -p /home/cloudnet
WORKDIR /home/cloudnet

COPY --from=build /usr/src/cloudnet-sources/cloudnet-launcher/build/libs/launcher.jar .
CMD ["java", "-XX:+UseG1GC", "-XX:MaxGCPauseMillis=50", "-XX:+UnlockExperimentalVMOptions", "-XX:+UseCompressedOops", "-XX:-UseAdaptiveSizePolicy", "-XX:CompileThreshold=100", "-Dfile.encoding=UTF-8", "-Xmx456M", "-Xms256m", "-jar", "launcher.jar"]
