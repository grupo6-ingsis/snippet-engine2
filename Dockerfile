FROM gradle:8.5.0-jdk21 AS build
ARG GPR_USER
ARG GPR_TOKEN
ENV GPR_USER=${GPR_USER} GPR_TOKEN=${GPR_TOKEN}

COPY  . /home/gradle/src
WORKDIR /home/gradle/src
RUN gradle assemble
FROM eclipse-temurin:21-jre
EXPOSE 8080
RUN mkdir /app
COPY --from=build /home/gradle/src/build/libs/*.jar /app/spring-boot-application.jar
ENTRYPOINT ["java", "-jar", "-Dspring.profiles.active=production","/app/spring-boot-application.jar"]