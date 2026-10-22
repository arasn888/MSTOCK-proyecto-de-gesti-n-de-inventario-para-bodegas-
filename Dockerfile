# Imagen de un microservicio Spring Boot. Un solo Dockerfile sirve para los nueve módulos:
#   docker build --build-arg SERVICE=auth-service -t mstock/auth-service .
# (docker compose ya lo hace por ti con "docker compose --profile full up -d --build")

# ---------- Etapa 1: compilar ----------
FROM eclipse-temurin:21-jdk AS construir
ARG SERVICE
WORKDIR /src

# Primero lo que casi no cambia (wrapper de Gradle y archivos de build) para aprovechar la caché de capas
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
COPY eureka-server/build.gradle ./eureka-server/
COPY config-server/build.gradle ./config-server/
COPY auth-service/build.gradle ./auth-service/
COPY catalogo-service/build.gradle ./catalogo-service/
COPY inventario-service/build.gradle ./inventario-service/
COPY ventas-service/build.gradle ./ventas-service/
COPY compras-service/build.gradle ./compras-service/
COPY notificaciones-service/build.gradle ./notificaciones-service/
COPY api-gateway/build.gradle ./api-gateway/
RUN chmod +x gradlew && ./gradlew --no-daemon -q help

# Luego el código del servicio elegido
COPY ${SERVICE}/src ./${SERVICE}/src
RUN ./gradlew --no-daemon :${SERVICE}:bootJar -x test \
 && cp "$(ls ${SERVICE}/build/libs/*.jar | grep -v -- '-plain.jar' | head -n 1)" /app.jar

# ---------- Etapa 2: imagen final, solo con el JRE ----------
FROM eclipse-temurin:21-jre
ARG PORT
# curl lo usa el healthcheck de docker compose
RUN apt-get update && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/* \
 && useradd --system --no-create-home mstock
COPY --from=construir /app.jar /app.jar
USER mstock
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -Xss512k"
EXPOSE ${PORT}
ENTRYPOINT ["java", "-jar", "/app.jar"]
