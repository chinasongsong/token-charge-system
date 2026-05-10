FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src

COPY . .
RUN mvn -B -ntp -pl user-center-service -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app

COPY --from=build /src/user-center-service/target/user-center-service-*.jar /app/app.jar

USER app
EXPOSE 8101
ENTRYPOINT ["java","-jar","/app/app.jar"]
