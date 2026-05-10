FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src

COPY . .
RUN mvn -B -ntp -pl ops-console -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app

COPY --from=build /src/ops-console/target/ops-console-*.jar /app/app.jar

USER app
EXPOSE 8105
ENTRYPOINT ["java","-jar","/app/app.jar"]
