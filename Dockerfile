# P1-B5: imagen segura.
# - Etapa de build: JDK 25 (solo para compilar).
# - Etapa de runtime: JRE 25 (sin compilador, menor superficie).
# - Usuario no root (appuser).
# - Los tests CORREN en el build (antes: ./gradlew build -x test los omitía).
# - Sin secretos en la imagen: toda credencial llega por variables de
#   entorno en tiempo de ejecución (ver application-example.properties).
# - HEALTHCHECK de puerto (liveness): comprueba que 8080 escucha.
#   No es readiness de la app: la imagen JRE no trae curl/wget y no hay
#   endpoint /actuator/health en este backend.
FROM eclipse-temurin:25-jdk-jammy AS builder
WORKDIR /app
COPY gradle gradle
COPY gradlew build.gradle.kts settings.gradle.kts ./
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true
COPY src src
RUN ./gradlew build --no-daemon

FROM eclipse-temurin:25-jre-jammy
RUN groupadd -r appgroup && useradd -r -g appgroup appuser
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
RUN chown appuser:appgroup app.jar
USER appuser
EXPOSE 8080
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD bash -c 'cat < /dev/null > /dev/tcp/127.0.0.1/8080' || exit 1
ENTRYPOINT ["java", "-Xmx300m", "-jar", "app.jar"]
