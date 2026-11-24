FROM node:24-bookworm-slim AS frontend
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY vite.config.js ./
COPY frontend frontend
COPY res/wheaton.otf res/wheaton.otf
RUN npm run build
RUN node --input-type=module -e '\
  import {createHash} from "node:crypto"; \
  import {writeFile} from "node:fs/promises"; \
  const path = "/maven2/org/postgresql/postgresql/42.7.14/postgresql-42.7.14.jar"; \
  const expected = "73914527305a40cce504b0d3d90b23caf565136912d607ae8ae7c5895512332c"; \
  for (const origin of ["https://repo.maven.apache.org", "https://maven-central.storage-download.googleapis.com"]) { \
    try { \
      const response = await fetch(origin + path, {signal: AbortSignal.timeout(15000)}); \
      if (!response.ok) throw new Error("HTTP " + response.status); \
      const bytes = Buffer.from(await response.arrayBuffer()); \
      if (createHash("sha256").update(bytes).digest("hex") !== expected) throw new Error("Checksum mismatch"); \
      await writeFile("postgresql.jar", bytes); \
      process.exit(0); \
    } catch (error) { console.error(origin, error.message); } \
  } \
  process.exit(1);'

FROM eclipse-temurin:17-jdk AS backend
WORKDIR /app
COPY backend/src src
RUN mkdir classes && javac --release 17 -encoding UTF-8 -d classes src/*.java

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=frontend /app/dist dist
COPY --from=backend /app/classes classes
COPY res res
COPY --from=frontend /app/postgresql.jar /app/lib/postgresql.jar
ENV BIND_ADDRESS=0.0.0.0 FRONTEND_DIR=dist PORT=8080
EXPOSE 8080
USER 10001:10001
CMD ["java", "-Djava.awt.headless=true", "-cp", "classes:lib/*", "WebServer"]
