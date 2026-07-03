# ---- Stage 1: Build the application with Maven ----
FROM maven:3.9-eclipse-temurin-21 AS build

WORKDIR /build

# Copy only the files needed to resolve dependencies first,
# so Docker can cache this layer across rebuilds when only source code changes.
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
RUN ./mvnw dependency:go-offline -B

# Now copy the source and build the jar
COPY src ./src
RUN ./mvnw package -DskipTests -B


# ---- Stage 2: Run the application with a slim JRE ----
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Run as a non-root user for better container security
RUN addgroup -S enerflow && adduser -S enerflow -G enerflow
USER enerflow

# Copy only the built jar from the build stage
COPY --from=build /build/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]