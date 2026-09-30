# Etapa 1: gera o .jar com Maven + JDK 21 (não depende de Java/Maven instalados na máquina)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

# Etapa 2: imagem final, só com o JRE e o .jar
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
ENV TZ=America/Sao_Paulo
COPY --from=build /app/target/*.jar app.jar
# Pasta dos anexos (montada como volume no docker-compose)
RUN mkdir -p /app/uploads
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
