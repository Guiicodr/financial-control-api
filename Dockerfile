# 1. Usa a imagem oficial do JDK 25 e instala o Maven
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app

# Instala o Maven no sistema
RUN apt-get update && apt-get install -y maven && rm -rf /var/lib/apt/lists/*

# 2. Copia todo o projeto
COPY . .

# 3. Dá permissão de execução ao Maven Wrapper
RUN chmod +x ./mvnw

# 4. Compila o projeto com Java 25 sem rodar testes
ENV MAVEN_OPTS="-Xmx512m"
RUN ./mvnw clean package -DskipTests -Dmaven.test.skip=true

# 5. Estágio final de execução com o Java 25 JRE
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]