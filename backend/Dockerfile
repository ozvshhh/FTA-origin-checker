# 백엔드 운영 이미지. 빌드: docker build -t origin-check-backend backend/
# ---- 1) 빌드 ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /src
# 의존성만 먼저 받아 두면 소스만 바뀐 빌드는 이 층을 다시 쓴다
COPY gradlew settings.gradle build.gradle ./
COPY gradle gradle
RUN ./gradlew --no-daemon -q dependencies > /dev/null
COPY src src
RUN ./gradlew --no-daemon -q bootJar -x test && cp build/libs/*.jar /app.jar

# ---- 2) 실행 ----
FROM eclipse-temurin:21-jre
RUN groupadd --system app && useradd --system --gid app --home /app app \
    && mkdir -p /data/uploads && chown -R app:app /data
WORKDIR /app
COPY --from=build /app.jar app.jar
USER app
ENV STORAGE_DIR=/data/uploads \
    SERVER_PORT=8080 \
    JAVA_OPTS="-XX:MaxRAMPercentage=60 -XX:+ExitOnOutOfMemoryError"
VOLUME /data/uploads
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=3s --start-period=60s --retries=5 \
    CMD bash -c 'exec 3<>/dev/tcp/127.0.0.1/${SERVER_PORT} && printf "GET /api/v1/health HTTP/1.0\r\n\r\n" >&3 && grep -q "200" <&3'
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
