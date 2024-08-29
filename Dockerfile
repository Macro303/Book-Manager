FROM --platform=$BUILDPLATFORM gradle:jdk17 AS builder

WORKDIR /data
COPY . /data/
RUN gradle build



FROM --platform=$TARGETPLATFORM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=builder /data/app/build/libs/app-0.4.1-all.jar /app/Bookshelf.jar
ENV XDG_CACHE_HOME=/app/cache \
    XDG_CONFIG_HOME=/app/config \
    XDG_DATA_HOME=/app/data
RUN mkdir -p $XDG_CONFIG_HOME/bookshelf $XDG_DATA_HOME/bookshelf

EXPOSE 25710
CMD ["java", "-jar", "Bookshelf.jar"]
