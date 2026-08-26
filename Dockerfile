# Estágio de Build usando Maven oficial com Java 21
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copia os arquivos do projeto
COPY . .

# Compila o projeto direto com o Maven da imagem (evita problemas com o script ./mvnw)
RUN mvn clean package -DskipTests

# Estágio de Execução (imagem minimalista)
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]