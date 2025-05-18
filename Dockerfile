# Stage 1: Cache Gradle dependencies
FROM gradle:3.4.1-jdk7-alpine AS cache
RUN mkdir -p /home/gradle/cache_home
ENV GRADLE_USER_HOME=/home/gradle/cache_home
COPY build.gradle.* gradle.properties /home/gradle/app/
COPY gradle /home/gradle/app/gradle
WORKDIR /home/gradle/app
RUN gradle clean build -i --stacktrace

# Stage 2: Build Application
FROM gradle:3.4.1-jdk7-alpine AS build
COPY --from=cache /home/gradle/cache_home /home/gradle/.gradle
COPY --chown=gradle:gradle . /home/gradle/src
WORKDIR /home/gradle/src
# Build the fat JAR, Gradle also supports shadow
# and boot JAR by default.
RUN gradle buildFatJar --no-daemon

# Stage 3: Create the Runtime Image
FROM openjdk:22-ea-17-oraclelinux8 AS runtime
EXPOSE 9999
RUN mkdir /app
# Needed environment variables for database connection
ENV DB_HOST=postgres
ENV DB_PORT=5432
ENV DB_NAME=keyman
ENV PORT=9999
COPY --from=build /home/gradle/src/build/libs/*.jar /app/ktor-keyman-server.jar
ENTRYPOINT ["java","-jar","-port","9999","/app/ktor-keyman-server.jar"]