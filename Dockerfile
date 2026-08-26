# 1. Usa a imagem do Maven com JDK 25 para compilar
FROM maven:3.9.9-eclipse-temurin-25 AS build
WORKDIR /app

# 2. Copia todo o projeto
COPY . .

# 3. Dá permissão de execução ao Maven Wrapper
RUN chmod +x ./mvnw

# 4. Define a memória limite e executa o empacotamento
ENV MAVEN_OPTS="-Xmx512m"
RUN ./mvnw clean package -DskipTests -Dmaven.test.skip=true

# 5. Estágio final de execução com o Java 25 Runtime
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]