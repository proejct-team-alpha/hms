# ---- Frontend Build Stage ----
FROM node:20-slim AS frontend
WORKDIR /app

COPY package.json package-lock.json* ./
RUN npm ci

COPY src/main/resources/static/ src/main/resources/static/
COPY src/main/resources/templates/ src/main/resources/templates/
COPY scripts/ scripts/
RUN npm run build

# ---- Backend Build Stage ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

COPY gradlew gradlew.bat ./
COPY gradle/ gradle/
RUN chmod +x gradlew

COPY build.gradle settings.gradle ./
RUN ./gradlew dependencies --no-daemon || true

COPY src/ src/
COPY --from=frontend /app/src/main/resources/static/ src/main/resources/static/
RUN ./gradlew bootJar --no-daemon -x test

# ---- Runtime Stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app

RUN mkdir -p /opt/hms/logs

COPY --from=build /app/build/libs/hms-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", \
  "-Dspring.profiles.active=prod", \
  "-jar", "app.jar"]
