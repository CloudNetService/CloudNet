ARG BRANCH="nightly"
ARG VERSION="0.0.0-dev"
ARG REVISION="local"
ARG CREATED="1970-01-01T00:00:00Z"

FROM --platform=$BUILDPLATFORM azul/zulu-openjdk-debian:25-latest AS builder

WORKDIR /build
COPY . .
RUN ./gradlew build genUpdaterInformation -x test -x checkstyleMain -x checkstyleTest --no-daemon

FROM azul/zulu-openjdk-alpine:25-jre-headless-latest
ARG BRANCH
ARG VERSION
ARG REVISION
ARG CREATED

LABEL org.opencontainers.image.version="${VERSION}"
LABEL org.opencontainers.image.created="${CREATED}"
LABEL org.opencontainers.image.revision="${REVISION}"

LABEL org.opencontainers.image.licenses="Apache-2.0"
LABEL org.opencontainers.image.vendor="CloudNetService"
LABEL org.opencontainers.image.authors="derklaro <derklaro@cloudnetservice.eu>"

LABEL org.opencontainers.image.title="cloudnet-node"
LABEL org.opencontainers.image.source="https://github.com/CloudNetService/CloudNet"
LABEL org.opencontainers.image.description="A modern application that can dynamically and easily deliver Minecraft oriented software"

USER root
RUN apk add --update --no-cache iproute2

RUN addgroup -S cloudnet && adduser -S cloudnet -G cloudnet
USER cloudnet
WORKDIR /home/cloudnet

COPY --from=builder --chown=cloudnet:cloudnet /build/node/impl/build/libs/cloudnet.jar ./
COPY --from=builder --chown=cloudnet:cloudnet /build/launcher/java22/build/libs/launcher.jar ./
COPY --from=builder --chown=cloudnet:cloudnet /build/.launchermeta/modules.json ./launcher/

# 1410: default internal network listener; port 2812: default rest-module port
EXPOSE 1410/tcp
EXPOSE 2812/tcp
HEALTHCHECK --interval=15s --start-period=15s --timeout=5s --retries=5 CMD ss -Hltn 'sport = :1410' | grep -q . || exit 1

ENV CLOUDNET_DEV=true
ENV CLOUDNET_UPDATEBRANCH=${BRANCH}
CMD ["java", "-Xms128M", "-Xmx128M", "-XX:+PerfDisableSharedMem", "-XX:+ExitOnOutOfMemoryError", "-jar", "launcher.jar"]
