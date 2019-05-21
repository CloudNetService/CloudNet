FROM gradle:5.4.1-jdk11 AS build
USER root

COPY . /usr/src/cloudnet-sources
WORKDIR /usr/src/cloudnet-sources

RUN gradle

FROM openjdk:8u212-jre-alpine3.9
USER root

COPY --from=build /usr/src/cloudnet-sources/cloudnet-launcher/build/libs/launcher.jar /home/cloudnet

CMD ["java", "-XX:CompileThreshold=100", "-Dfile.encoding=UTF-8", "-jar", "launcher.jar"]