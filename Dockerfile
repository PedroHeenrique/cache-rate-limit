# Stage 1: Build
FROM maven:3.9.2-eclipse-temurin-17 AS build

# Define diretório de trabalho
WORKDIR /app

# Copia o pom primeiro e faz o download das dependências
COPY pom.xml .
RUN mvn dependency:go-offline

# Copia o restante do código
COPY src ./src

# Build do JAR
RUN mvn clean package -DskipTests

# Stage 2: Runtime
FROM eclipse-temurin:17-jdk-alpine

# Copia o JAR do stage de build
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Usar usuário não-root
USER 100

# Comando para rodar a aplicação
ENTRYPOINT ["java", "-jar", "/app/app.jar"]