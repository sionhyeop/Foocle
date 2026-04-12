FROM eclipse-temurin:17-jdk

ARG JAR_FILE=build/libs/*.jar

COPY ${JAR_FILE} app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-Dspring.profiles.active=deploy", "-jar", "/app.jar"]
