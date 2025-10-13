# Usa una JDK base
FROM eclipse-temurin:21-jdk-jammy

# Set working directory
WORKDIR /app

# Copia i file Maven
COPY pom.xml mvnw ./
COPY .mvn .mvn

# Scarica le dipendenze Maven (caching layer)
RUN ./mvnw dependency:go-offline

# Copia il resto del progetto
COPY src ./src

# Build del JAR
RUN ./mvnw clean package -DskipTests

# Espone la porta
EXPOSE 8080

# Comando per avviare Spring Boot
CMD ["java", "-jar", "target/atlas-0.0.1-SNAPSHOT.jar"]