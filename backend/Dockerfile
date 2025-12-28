FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -q -e -DskipTests -Dspring-boot.repackage.skip=true package
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/plms-0.0.1-SNAPSHOT.jar /app/plms.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/plms.jar"]
