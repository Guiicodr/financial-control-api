# Estágio de Build usando Maven oficial com Java 21
FROM maven:3.9.6-eclipse-temurin-21 AS build
WORKDIR /app

# Copia apenas o necessário para resolver dependências. Separar o pom.xml do
# resto do código permite reaproveitar a camada de dependências do Maven entre
# builds (o `COPY . .` antigo invalidava o cache a cada mudança de arquivo).
COPY pom.xml .
# O `|| true` é proposital: se o go-offline falhar (dependência opcional,
# repositório temporariamente fora), o build seguinte baixa o que falta em vez
# de quebrar o deploy.
RUN mvn -B -q dependency:go-offline || true

# Agora sim o código-fonte e o build.
COPY src ./src
RUN mvn -B clean package -DskipTests

# Estágio de Execução (imagem minimalista)
FROM eclipse-temurin:21-jre
WORKDIR /app

# Não rodar como root: se alguém explorar a aplicação, o processo não tem
# permissão de escrita no sistema de arquivos da imagem.
RUN groupadd --system --gid 1001 app \
    && useradd --system --uid 1001 --gid app --no-create-home app

COPY --from=build --chown=app:app /app/target/*.jar app.jar

USER app

EXPOSE 8080

# MaxRAMPercentage deixa a JVM respeitar o limite de memória do container
# (sem isso o Railway mata o container por OOM sem log claro).
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-XX:+UseSerialGC", "-Djava.security.egd=file:/dev/./urandom", "-jar", "app.jar"]
