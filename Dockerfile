FROM maven:3.9.16-eclipse-temurin-25 AS builder
WORKDIR /build

# Cache Maven dependencies by copying pom.xml first
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build the application
COPY src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:25-jre-jammy AS extractor
WORKDIR /extract
COPY --from=builder /build/target/*.jar app.jar
RUN java -Djarmode=tools -jar app.jar extract --layers --launcher

FROM eclipse-temurin:25-jre-jammy AS runtime
WORKDIR /app

# Create a non-root system user for security compliance
RUN useradd --system --uid 1001 --shell /bin/false springuser
USER springuser

# Copy extracted application layers from Stage 2
COPY --from=extractor /extract/app/dependencies/ ./
COPY --from=extractor /extract/app/spring-boot-loader/ ./
COPY --from=extractor /extract/app/snapshot-dependencies/ ./
COPY --from=extractor /extract/app/application/ ./

EXPOSE 8080

# Configure production-ready JVM flags
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "org.springframework.boot.loader.launch.JarLauncher"]
