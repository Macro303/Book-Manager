FROM eclipse-temurin:17-jdk AS builder

WORKDIR /builder
COPY . /builder/
RUN ./gradlew build



FROM eclipse-temurin:17-jre

WORKDIR /app
COPY --from=builder /builder/app/build/libs/app-0.4.0-all.jar /app/Bookshelf.jar
ENV XDG_CACHE_HOME=/app/cache \
    XDG_CONFIG_HOME=/app/config \
    XDG_DATA_HOME=/app/data
RUN mkdir -p $XDG_CONFIG_HOME/bookshelf $XDG_DATA_HOME/bookshelf

EXPOSE 25710
CMD ["java", "-jar", "Bookshelf.jar"]
