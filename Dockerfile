# Stage 1: Build file JAR bằng Maven
FROM maven:3.8.5-openjdk-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
# Build bỏ qua test để chạy nhanh hơn
RUN mvn clean package -DskipTests

# Stage 2: Chạy ứng dụng bằng OpenJDK 17
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
