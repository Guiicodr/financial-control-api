# 1. Usa a imagem do Maven com JDK 17 para compilar
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# 2. Copia todo o projeto
COPY . .

# 3. Dá permissão de execução ao Maven Wrapper
RUN chmod +x ./mvnw

# 4. Define a variável de memória corretamente e executa a compilação sem testes
ENV MAVEN_OPTS="-Xmx512m"
RUN ./mvnw clean package -DskipTests -Dmaven.test.skip=true

# 5. Estágio final de execução (apenas com o Java Runtime)
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]