# 多阶段构建：Gradle 打包 → JRE 运行
FROM gradle:8.2-jdk17-alpine AS build
WORKDIR /workspace
COPY settings.gradle.kts build.gradle.kts ./
COPY gradle ./gradle
COPY gradlew gradlew.bat ./
COPY pac-maze-engine ./pac-maze-engine
COPY pac-maze-server ./pac-maze-server
COPY app/src/main/java/com/example/funlife/social/game/engine/pacmaze ./app/src/main/java/com/example/funlife/social/game/engine/pacmaze
COPY app/src/main/assets/pac_maze/arenas ./app/src/main/assets/pac_maze/arenas
RUN chmod +x gradlew && ./gradlew :pac-maze-server:installDist --no-daemon -x test

FROM eclipse-temurin:17-jre-alpine
RUN apk add --no-cache wget
WORKDIR /app
COPY --from=build /workspace/pac-maze-server/build/install/pac-maze-server /app
ENV PB_BASE_URL=http://pocketbase:8090
ENV PAC_MAZE_WS_PORT=8791
ENV PAC_MAZE_DISCONNECT_MS=30000
ENV PAC_MAZE_ROOM_GO_MS=8000
EXPOSE 8791
HEALTHCHECK --interval=30s --timeout=5s --start-period=20s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8791/health || exit 1
ENTRYPOINT ["/app/bin/pac-maze-server"]
