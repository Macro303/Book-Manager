# Use official Gradle image as base
FROM gradle:latest as builder

# Set the working directory
WORKDIR /app

# Copy only the necessary files to cache dependencies
COPY build.gradle.kts settings.gradle.kts /app/
COPY src /app/src

# Build the application
RUN gradle build

# Use official OpenJDK image as base
FROM eclipse-temurin:17-jre-alpine

# Set the working directory
WORKDIR /app

# Copy the JAR file from the builder stage
COPY --from=builder /app/build/libs/Bookshelf-fatJar.jar /app/Bookshelf.jar

# Create XDG directories for external configuration and data
ENV XDG_CACHE_HOME /app/cache
ENV XDG_CONFIG_HOME /app/config
ENV XDG_DATA_HOME /app/data
RUN mkdir -p $XDG_CACHE_HOME/bookshelf && mkdir -p $XDG_CONFIG_HOME/bookshelf && mkdir -p $XDG_DATA_HOME/bookshelf

# Expose the port your web server will run on
EXPOSE 25710

# Command to run the application
CMD ["java", "-jar", "Bookshelf.jar"]
