# ── Build stage ────────────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jdk-noble AS build

WORKDIR /build
COPY pom.xml .
COPY src ./src

# Ajout du chart Helm dans l'image (build)
COPY k8s /build/chart

RUN apt-get update -qq && apt-get install -y -qq maven \
 && mvn package -q -DskipTests \
 && mv target/arc-consolidator-*.jar target/arc-consolidator.jar

# ── Runtime stage ───────────────────────────────────────────────────────────────
FROM eclipse-temurin:25-jre-noble

# Utilisateur non-root
RUN useradd -r appuser
USER appuser

WORKDIR /app

COPY --from=build /build/target/arc-consolidator.jar arc-consolidator.jar
# Ajout du chart Helm dans l'image (runtime)
COPY --from=build /build/chart ./chart

ENTRYPOINT ["java", "-jar", "arc-consolidator.jar"]
