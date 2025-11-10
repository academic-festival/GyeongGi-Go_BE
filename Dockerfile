FROM eclipse-temurin:21-jdk
COPY build/libs/*.jar app.jar
COPY src/main/resources/application.properties /app/resources/
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app.jar"]