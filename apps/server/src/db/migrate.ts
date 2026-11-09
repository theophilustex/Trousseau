import { sqlite } from './index.js';

export function runMigrations() {
  // Execute table creation statements idempotently
  sqlite.exec(`
    CREATE TABLE IF NOT EXISTS users (
      id TEXT PRIMARY KEY,
      username TEXT NOT NULL UNIQUE,
      password_hash TEXT NOT NULL,
      display_name TEXT NOT NULL,
      email TEXT NOT NULL UNIQUE,
      location_city TEXT,
      location_lat REAL,
      location_lon REAL,
      created_at TEXT NOT NULL,
      updated_at TEXT NOT NULL
    );

    CREATE TABLE IF NOT EXISTS clothing_items (
      id TEXT PRIMARY KEY,
      user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      name TEXT NOT NULL,
      category TEXT NOT NULL,
      subcategory TEXT,
      brand TEXT,
      size TEXT,
      colors TEXT NOT NULL DEFAULT '[]',
      season TEXT NOT NULL DEFAULT 'all_season',
      tags TEXT NOT NULL DEFAULT '[]',
      image_url TEXT,
      thumbnail_url TEXT,
      has_transparent_bg INTEGER NOT NULL DEFAULT 0,
      wears_since_wash INTEGER NOT NULL DEFAULT 0,
      max_wears_before_wash INTEGER NOT NULL DEFAULT 2,
      total_lifetime_wears INTEGER NOT NULL DEFAULT 0,
      total_washes INTEGER NOT NULL DEFAULT 0,
      status TEXT NOT NULL DEFAULT 'clean',
      min_temp_c REAL,
      max_temp_c REAL,
      rain_friendly INTEGER NOT NULL DEFAULT 0,
      notes TEXT,
      last_worn_at TEXT,
      created_at TEXT NOT NULL,
      updated_at TEXT NOT NULL
    );

    CREATE TABLE IF NOT EXISTS outfits (
      id TEXT PRIMARY KEY,
      user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      name TEXT NOT NULL,
      occasion TEXT NOT NULL DEFAULT 'casual',
      season TEXT NOT NULL DEFAULT 'all_season',
      is_public INTEGER NOT NULL DEFAULT 0,
      item_ids TEXT NOT NULL DEFAULT '[]',
      wear_count INTEGER NOT NULL DEFAULT 0,
      last_worn_at TEXT,
      created_at TEXT NOT NULL,
      updated_at TEXT NOT NULL
    );

    CREATE TABLE IF NOT EXISTS wear_logs (
      id TEXT PRIMARY KEY,
      user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      outfit_id TEXT REFERENCES outfits(id) ON DELETE SET NULL,
      item_ids TEXT NOT NULL DEFAULT '[]',
      date TEXT NOT NULL,
      status TEXT NOT NULL DEFAULT 'planned',
      notes TEXT,
      confirmed_at TEXT,
      created_at TEXT NOT NULL,
      updated_at TEXT NOT NULL
    );

    CREATE TABLE IF NOT EXISTS ratings (
      id TEXT PRIMARY KEY,
      outfit_id TEXT NOT NULL REFERENCES outfits(id) ON DELETE CASCADE,
      user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      rater_type TEXT NOT NULL,
      rater_name TEXT NOT NULL,
      stars INTEGER NOT NULL,
      feedback TEXT,
      created_at TEXT NOT NULL,
      updated_at TEXT NOT NULL
    );

    CREATE TABLE IF NOT EXISTS comments (
      id TEXT PRIMARY KEY,
      outfit_id TEXT NOT NULL REFERENCES outfits(id) ON DELETE CASCADE,
      user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
      user_name TEXT NOT NULL,
      content TEXT NOT NULL,
      created_at TEXT NOT NULL,
      updated_at TEXT NOT NULL
    );

    CREATE TABLE IF NOT EXISTS weather_cache (
      id TEXT PRIMARY KEY,
      city TEXT,
      lat REAL NOT NULL,
      lon REAL NOT NULL,
      date TEXT NOT NULL,
      temp_current_c REAL NOT NULL,
      temp_high_c REAL NOT NULL,
      temp_low_c REAL NOT NULL,
      precipitation_probability REAL NOT NULL,
      weather_code INTEGER NOT NULL,
      description TEXT NOT NULL,
      is_raining INTEGER NOT NULL,
      fetched_at TEXT NOT NULL
    );

    CREATE INDEX IF NOT EXISTS idx_clothing_items_user_id ON clothing_items(user_id);
    CREATE INDEX IF NOT EXISTS idx_clothing_items_category ON clothing_items(category);
    CREATE INDEX IF NOT EXISTS idx_clothing_items_status ON clothing_items(status);
    CREATE INDEX IF NOT EXISTS idx_outfits_user_id ON outfits(user_id);
    CREATE INDEX IF NOT EXISTS idx_wear_logs_user_date ON wear_logs(user_id, date);
    CREATE INDEX IF NOT EXISTS idx_ratings_outfit_id ON ratings(outfit_id);
    CREATE INDEX IF NOT EXISTS idx_comments_outfit_id ON comments(outfit_id);
  `);
}

// Run if called directly
if (process.argv[1]?.endsWith('migrate.ts')) {
  runMigrations();
  console.log('Database tables verified and migrated successfully.');
}
