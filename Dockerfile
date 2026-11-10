# ---------------------------------------------------
# Stage 1: Build the Monorepo
# ---------------------------------------------------
FROM node:22-bookworm-slim AS builder

WORKDIR /app

# Install build dependencies for better-sqlite3 and sharp native C++ modules
RUN apt-get update && apt-get install -y --no-install-recommends \
    python3 \
    make \
    g++ \
    && rm -rf /var/lib/apt/lists/*

RUN corepack enable && corepack prepare pnpm@latest --activate

# Copy manifest files
COPY package.json pnpm-workspace.yaml pnpm-lock.yaml ./
COPY packages/shared/package.json ./packages/shared/
COPY apps/server/package.json ./apps/server/
COPY apps/web/package.json ./apps/web/

# Install all dependencies and compile native addons
RUN pnpm install --frozen-lockfile
RUN pnpm rebuild better-sqlite3 sharp

# Sanity check: Ensure better_sqlite3.node was compiled successfully
RUN ls -la /app/node_modules/.pnpm/better-sqlite3@*/node_modules/better-sqlite3/build/Release/better_sqlite3.node

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
# Stage 2: Production Image
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

# Copy pre-compiled node_modules and native bindings directly from builder
COPY --from=builder /app/node_modules ./node_modules
COPY --from=builder /app/package.json ./package.json
COPY --from=builder /app/pnpm-workspace.yaml ./pnpm-workspace.yaml
COPY --from=builder /app/packages/shared ./packages/shared
COPY --from=builder /app/apps/server/dist ./apps/server/dist
COPY --from=builder /app/apps/server/node_modules ./apps/server/node_modules
COPY --from=builder /app/apps/server/web-dist ./apps/server/web-dist
COPY --from=builder /app/apps/server/package.json ./apps/server/package.json

# Final sanity check: verify binding exists in runner image
RUN ls -la /app/node_modules/.pnpm/better-sqlite3@*/node_modules/better-sqlite3/build/Release/better_sqlite3.node

# Create persistent data volume directory
RUN mkdir -p /app/data/uploads/images /app/data/uploads/thumbnails

VOLUME ["/app/data"]

EXPOSE 4000

HEALTHCHECK --interval=30s --timeout=5s --start-period=5s --retries=3 \
  CMD curl -f http://localhost:4000/api/health || exit 1

WORKDIR /app/apps/server
CMD ["node", "dist/index.js"]
