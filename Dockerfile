FROM gradle:latest as builder

WORKDIR /builder

COPY build.gradle.kts settings.gradle.kts .editorconfig /builder/
COPY gradle/libs.versions.toml /builder/gradle/
COPY app/build.gradle.kts /builder/app/
COPY app/src /builder/app/src
COPY openlibrary/build.gradle.kts openlibrary/cache.sqlite /builder/openlibrary/
COPY openlibrary/src /builder/openlibrary/src

RUN gradle build

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /app/app/build/libs/app-0.4.0-all.jar /app/Bookshelf.jar

ENV XDG_CACHE_HOME /app/cache
ENV XDG_CONFIG_HOME /app/config
RUN mkdir -p $XDG_CONFIG_HOME/bookshelf
ENV XDG_DATA_HOME /app/data
RUN mkdir -p $XDG_DATA_HOME/bookshelf

EXPOSE 25710
CMD ["java", "-jar", "Bookshelf.jar"]
