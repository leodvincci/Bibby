#Build Image
FROM maven:3-openjdk-21 AS stage

WORKDIR /app
COPY pom.xml .

RUN [ "mvn" "package"]

COPY src/ .

RUN mvn -B spotless:check
RUN mvn -B clean package

#Run Image
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
CMD ["java","-jar","app.jar"]