FROM eclipse-temurin:17-jdk-jammy AS build

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B dependency:go-offline

COPY src/ src/
RUN ./mvnw -B clean package -DskipTests

FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

ENV PORT=8085

RUN apt-get update \
    && apt-get install -y --no-install-recommends python3 python3-venv \
    && rm -rf /var/lib/apt/lists/*

COPY ai_service/ /app/ai_service/
RUN python3 -m venv /app/ai_service/.venv \
    && /app/ai_service/.venv/bin/python -m pip install --no-cache-dir -r /app/ai_service/requirements.txt \
    && printf "ok" > /app/ai_service/.venv/.requirements-installed

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
