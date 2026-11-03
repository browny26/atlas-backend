# ---- Build stage: full JDK + Maven ----
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app

# Dependencies first: this layer is cached until pom.xml changes.
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN ./mvnw -q dependency:go-offline

COPY src ./src
RUN ./mvnw -q clean package -DskipTests

# ---- Runtime stage: JRE only (smaller image, smaller attack surface) ----
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
RUN useradd --create-home --uid 1000 spring
COPY --from=build /app/target/atlas-0.0.1-SNAPSHOT.jar app.jar
USER spring

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
