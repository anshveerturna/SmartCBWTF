# Build image
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -B -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

# Runtime image
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/target/backend-0.0.1-SNAPSHOT.jar app.jar
RUN useradd --system --create-home --uid 10001 appuser \
    && mkdir -p \
        /app/files/agreements \
        /app/uploads/profiles \
        /app/uploads/payment-qr \
        /app/uploads/branding \
        /app/uploads/rent-agreements \
        /app/uploads/consumables \
    && chown -R appuser:appuser /app
USER appuser
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75"
EXPOSE 8080
ENTRYPOINT ["sh","-c","exec java ${JAVA_OPTS} -jar /app/app.jar"]
