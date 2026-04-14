# syntax=docker/dockerfile:1.6
# ───────── build stage ─────────
FROM eclipse-temurin:17-jdk AS build
WORKDIR /workspace

# 의존성 캐시 히트를 위해 pom 을 먼저 복사
COPY pom.xml .
RUN --mount=type=cache,target=/root/.m2 \
	mvn -B -q -pl . dependency:go-offline -DskipTests || true

COPY src ./src
RUN --mount=type=cache,target=/root/.m2 \
	mvn -B -q -DskipTests package && \
	cp target/*.jar /workspace/app.jar

# ───────── runtime stage ─────────
FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app

# non-root 유저로 실행
RUN groupadd --system app && useradd --system --gid app --home /app app
COPY --from=build --chown=app:app /workspace/app.jar /app/app.jar

USER app
EXPOSE 8080

ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
