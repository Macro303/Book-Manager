FROM eclipse-temurin:17-jre

WORKDIR /app

COPY build/libs/Bookshelf-fatJar.jar /app/Bookshelf.jar

ENV XDG_CACHE_HOME /app/cache
ENV XDG_CONFIG_HOME /app/config
RUN mkdir -p $XDG_CONFIG_HOME/bookshelf
ENV XDG_DATA_HOME /app/data
RUN mkdir -p $XDG_DATA_HOME/bookshelf

EXPOSE 25710

CMD ["java", "-jar", "Bookshelf.jar"]
