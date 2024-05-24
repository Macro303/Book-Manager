FROM gradle:latest as builder

WORKDIR /app

COPY build.gradle.kts settings.gradle.kts /app/
COPY src /app/src

RUN gradle build

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=builder /app/build/libs/Bookshelf-fat.jar /app/Bookshelf.jar

ENV XDG_CACHE_HOME /app/cache
ENV XDG_CONFIG_HOME /app/config
RUN mkdir -p $XDG_CONFIG_HOME/bookshelf
ENV XDG_DATA_HOME /app/data
RUN mkdir -p $XDG_DATA_HOME/bookshelf

EXPOSE 25710

CMD ["java", "-jar", "Bookshelf.jar"]
