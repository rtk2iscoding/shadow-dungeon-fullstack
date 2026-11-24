FROM node:24-bookworm-slim AS frontend
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY vite.config.js ./
COPY frontend frontend
COPY res/wheaton.otf res/wheaton.otf
RUN npm run build

FROM eclipse-temurin:17-jdk AS backend
WORKDIR /app
COPY backend/src src
RUN mkdir classes && javac --release 17 -encoding UTF-8 -d classes src/*.java

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=frontend /app/dist dist
COPY --from=backend /app/classes classes
COPY res res
ENV BIND_ADDRESS=0.0.0.0 FRONTEND_DIR=dist PORT=8080
EXPOSE 8080
USER 10001:10001
CMD ["java", "-Djava.awt.headless=true", "-cp", "classes", "WebServer"]
