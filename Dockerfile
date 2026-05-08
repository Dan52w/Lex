# Etapa 1: Compilar con Gradle usando Java 25
FROM eclipse-temurin:25-jdk-jammy AS build
WORKDIR /app

# Copiamos archivos de configuración
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

# Permisos y descarga de dependencias
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon

# Copiamos código y construimos
COPY src src
RUN ./gradlew build -x test --no-daemon

# Etapa 2: Imagen de ejecución con Java 25
FROM eclipse-temurin:25-jdk-jammy
WORKDIR /app
# Copiamos el jar generado
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
# Mantenemos el límite de memoria para Render
ENTRYPOINT ["java", "-Xmx300m", "-jar", "app.jar"]