FROM maven:3.9.11-eclipse-temurin-21 AS build

COPY . /app/
WORKDIR /app

RUN mvn clean package -DskipTests

FROM eclipse-temurin:21.0.8_9-jre-alpine-3.22

COPY --from=build /app/target/talk-flow-0.0.1-SNAPSHOT.jar talkflow.jar

EXPOSE 9090
CMD ["java","-jar", "talkflow.jar"]