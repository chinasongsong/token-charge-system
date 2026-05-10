FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src

COPY . .
RUN mvn -B -ntp -pl billing-service -am package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app

COPY --from=build /src/billing-service/target/billing-service-*.jar /app/app.jar

USER app
EXPOSE 8103
ENTRYPOINT ["java","-jar","/app/app.jar"]
