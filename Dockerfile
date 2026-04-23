FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /app
COPY pom.xml .
COPY idea-island-types/pom.xml idea-island-types/pom.xml
COPY idea-island-api/pom.xml idea-island-api/pom.xml
COPY idea-island-domain/pom.xml idea-island-domain/pom.xml
COPY idea-island-infrastructure/pom.xml idea-island-infrastructure/pom.xml
COPY idea-island-trigger/pom.xml idea-island-trigger/pom.xml
COPY idea-island-app/pom.xml idea-island-app/pom.xml
# RUN mvn dependency:go-offline -q
COPY . .
RUN --mount=type=cache,target=/root/.m2,id=idea-island-m2 \
        mvn -B -Dmaven.test.skip=true clean package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
COPY --from=builder /app/idea-island-app/target/idea-island-app.jar app.jar
RUN mkdir -p /app/logs /app/data/log && chown -R appuser:appgroup /app
USER appuser
EXPOSE 8091
ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS:--Xms256m -Xmx256m -XX:+UseG1GC -Dfile.encoding=UTF-8 -Dspring.profiles.active=prod} -jar app.jar"]
