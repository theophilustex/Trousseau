# Trousseau

**Trousseau** is a privacy-first, self-hostable wardrobe catalog, outfit builder, wear/wash tracker, and styling assistant with weather-aware recommendations and a companion mobile app.

---

## ✨ Features

- 👔 **Wardrobe Management**: Catalog clothes with photos, categories (tops, bottoms, outerwear, footwear, accessories, one-piece, undergarments), brands, sizes, colors, and custom tags.
- ✂️ **Private Local Background Removal**: Clean cutout silhouettes for clothes processed 100% locally on your host with zero external cloud calls.
- 🎨 **Outfit Builder & Canvas**: Combine items into visual looks for different occasions (*casual, formal, business, sport, date night, party, loungewear*) with season tagging.
- 🧺 **Wear & Wash Tracking**: Tracks how many times each item is worn. Items automatically flip to `Needs Wash` when their customizable wear threshold is exceeded (e.g. 2 wears for shirts, 4 for jeans, 15 for jackets).
- 🧼 **Laundry Basket & 1-Click Wash**: See dirty clothes in one place and reset wear counters individually or via "Wash All Dirty".
- 🌤️ **Weather Consideration**: Recommendations match your local temperature and precipitation using **Open-Meteo** (open source, no API key required, zero tracking).
- 🔄 **Diversity & Anti-Repetition Engine**: Penalizes recently worn outfits and items to prevent wearing the same clothes in a short window while cycling through underutilized favorites.
- 🧼 **Cleanliness Gate**: Outfits containing dirty items are strictly excluded from daily recommendations.
- ⭐ **Multi-Perspective Ratings**: Rate outfits across three distinct categories:
  - **Personal Assessment** (1–5 stars)
  - **Spouse / Partner Feedback** (1–5 stars)
  - **Friends & Peer Opinions** (1–5 stars)
- 💬 **Style Comments & Community Sharing**: Leave notes on outfits to discuss styling choices or share outfits with household members on your instance.
- 📱 **Companion Mobile App**: Glances at today's recommended outfit, alerts you to rain, and features a crucial **1-Tap Commitment ("I Wore This Today")** or quick outfit swapper.
- 📦 **1-Click Backup & Portability**: Export your entire wardrobe, database records, wear logs, ratings, and original photos into a portable `.zip` archive, or restore it onto another instance.

---

## 🏗️ System Architecture

Trousseau is architected as a lightweight, modular TypeScript monorepo:

```
Trousseau/
├── packages/
│   └── shared/          # Types, Zod schemas, constants, and validation
├── apps/
│   ├── server/          # Fastify API, SQLite (better-sqlite3 + Drizzle ORM), Sharp
│   ├── web/             # Responsive React 18, Vite, Tailwind CSS, Lucide icons
│   └── mobile/          # React Native & Expo companion app with APK build config
├── data/                # Persistent SQLite database and uploaded images
├── Dockerfile           # Turnkey multi-stage production container
└── docker-compose.yml   # Single-command self-hosting deployment
```

---

## 🚀 Quick Start (Self-Hosting with Docker)

### 1. Run with Docker Compose
Clone the repository and run:

```bash
docker compose up -d --build
```

Access the web interface at **`http://localhost:4000`**.

### 2. Configuration
Data and uploaded photos are stored in `./data`. You can customize environment variables in `docker-compose.yml` or `.env`:

```env
PORT=4000
HOST=0.0.0.0
DATA_DIR=/app/data
JWT_SECRET=your_random_secure_secret_string
```

---

## 💻 Local Development

### Prerequisites
- Node.js v20+ or v22+
- `pnpm` (Corepack enabled: `corepack enable`)

### Setup & Run
```bash
# 1. Install dependencies across monorepo
pnpm install

# 2. Build shared package
pnpm --filter @trousseau/shared run build

# 3. Run server & web concurrently
pnpm dev
```
- **Web App**: `http://localhost:3000` (proxies `/api` and `/uploads` to port 4000)
- **Backend API**: `http://localhost:4000`

### Running Automated Tests
```bash
pnpm --filter @trousseau/server run test
```

---

## 📱 Companion Mobile App & APK Generation

The mobile app is located in `apps/mobile` and connects to your Trousseau server via LAN IP (e.g. `http://192.168.1.50:4000`), reverse proxy domain, or Tailscale IP.

### Generating an Android APK

#### Method 1: Using EAS Build (Cloud / Fastest)
```bash
cd apps/mobile
npm install -g eas-cli
eas login
eas build -p android --profile preview
```
This generates a direct `.apk` file ready to sideload on any Android device.

#### Method 2: Local Offline Gradle Build
```bash
cd apps/mobile
npx expo prebuild --platform android
cd android
./gradlew assembleRelease
```
The compiled APK will be generated at:
`apps/mobile/android/app/build/outputs/apk/release/app-release.apk`

---

## 🔒 Privacy & Open Source Principles

- **Zero External Telemetry**: No tracking scripts, analytics, or third-party tracking cookies.
- **Local SQLite Storage**: Uses SQLite with WAL (Write-Ahead Logging) for high performance and durability; all database records and images remain strictly on your disk.
- **Privacy-Friendly Weather**: Weather forecasts are queried directly from Open-Meteo without registration or API keys and cached locally to minimize network calls.

---

## 📄 License
Open source under the MIT License.
