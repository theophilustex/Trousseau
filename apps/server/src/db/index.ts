import Database from 'better-sqlite3';
import { drizzle } from 'drizzle-orm/better-sqlite3';
import * as schema from './schema.js';
import fs from 'node:fs';
import path from 'node:path';

const DATA_DIR = process.env.DATA_DIR || path.resolve(process.cwd(), 'data');
const UPLOADS_DIR = path.resolve(DATA_DIR, 'uploads');
const IMAGES_DIR = path.resolve(UPLOADS_DIR, 'images');
const THUMBNAILS_DIR = path.resolve(UPLOADS_DIR, 'thumbnails');
const DB_PATH = process.env.DATABASE_PATH || path.resolve(DATA_DIR, 'trousseau.sqlite');

// Ensure necessary directories exist
fs.mkdirSync(DATA_DIR, { recursive: true });
fs.mkdirSync(IMAGES_DIR, { recursive: true });
fs.mkdirSync(THUMBNAILS_DIR, { recursive: true });

export const sqlite = new Database(DB_PATH);

// Configure SQLite for high performance and integrity in self-hosted environments
sqlite.pragma('journal_mode = WAL');
sqlite.pragma('foreign_keys = ON');
sqlite.pragma('busy_timeout = 5000');
sqlite.pragma('synchronous = NORMAL');

export const db = drizzle(sqlite, { schema });

export const storagePaths = {
  dataDir: DATA_DIR,
  uploadsDir: UPLOADS_DIR,
  imagesDir: IMAGES_DIR,
  thumbnailsDir: THUMBNAILS_DIR,
  dbPath: DB_PATH
};
