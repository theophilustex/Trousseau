# ---------------------------------------------------
# Stage 1: Build the Monorepo
# ---------------------------------------------------
FROM node:22-bookworm-slim AS builder

WORKDIR /app

# Install build dependencies for better-sqlite3 and sharp native modules
RUN apt-get update && apt-get install -y --no-install-recommends \
    python3 \
    make \
    g++ \
    && rm -rf /var/lib/apt/lists/*

RUN corepack enable && corepack prepare pnpm@10.2.1 --activate

# Copy manifest files
COPY package.json pnpm-workspace.yaml pnpm-lock.yaml ./
COPY packages/shared/package.json ./packages/shared/
COPY apps/server/package.json ./apps/server/
COPY apps/web/package.json ./apps/web/

# Install all dependencies
RUN pnpm install --frozen-lockfile

# Copy source code
COPY packages/shared ./packages/shared
COPY apps/server ./apps/server
COPY apps/web ./apps/web

# Build shared, web, and server
RUN pnpm --filter @trousseau/shared run build
RUN pnpm --filter @trousseau/web run build
RUN pnpm --filter @trousseau/server run build

# Copy web dist into server for self-contained serving
RUN cp -r apps/web/dist apps/server/web-dist

# ---------------------------------------------------
# Stage 2: Minimal Production Image
# ---------------------------------------------------
FROM node:22-bookworm-slim AS runner

WORKDIR /app

ENV NODE_ENV=production
ENV PORT=4000
ENV HOST=0.0.0.0
ENV DATA_DIR=/app/data

RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    && rm -rf /var/lib/apt/lists/*

RUN corepack enable && corepack prepare pnpm@10.2.1 --activate

# Copy workspace manifests
COPY package.json pnpm-workspace.yaml pnpm-lock.yaml ./
COPY packages/shared/package.json ./packages/shared/
COPY apps/server/package.json ./apps/server/

# Install only production dependencies
RUN pnpm install --prod --frozen-lockfile

# Copy compiled artifacts
COPY --from=builder /app/packages/shared/dist ./packages/shared/dist
COPY --from=builder /app/apps/server/dist ./apps/server/dist
COPY --from=builder /app/apps/server/src/db ./apps/server/dist/db
COPY --from=builder /app/apps/server/web-dist ./apps/server/web-dist

# Create persistent data volume directory
RUN mkdir -p /app/data/uploads/images /app/data/uploads/thumbnails

VOLUME ["/app/data"]

EXPOSE 4000

HEALTHCHECK --interval=30s --timeout=5s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:4000/api/health || exit 1

WORKDIR /app/apps/server
CMD ["node", "dist/index.js"]
