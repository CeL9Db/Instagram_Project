# Etape 1 : on compile le serveur avec Maven
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests -B

# Etape 2 : on lance juste le .jar avec Java 17
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# port du serveur (socket TCP pour le client JavaFX)
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]