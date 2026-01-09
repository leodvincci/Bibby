#Build Image
FROM maven:3-eclipse-temurin-17 AS build

WORKDIR /app
COPY pom.xml .
COPY src/ src/

RUN mvn -B clean package

#Run Image
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
CMD ["java","-jar","app.jar"]