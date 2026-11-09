import { sqliteTable, text, integer, real } from 'drizzle-orm/sqlite-core';

export const users = sqliteTable('users', {
  id: text('id').primaryKey(),
  username: text('username').notNull().unique(),
  passwordHash: text('password_hash').notNull(),
  displayName: text('display_name').notNull(),
  email: text('email').notNull().unique(),
  locationCity: text('location_city'),
  locationLat: real('location_lat'),
  locationLon: real('location_lon'),
  createdAt: text('created_at').notNull(),
  updatedAt: text('updated_at').notNull()
});

export const clothingItems = sqliteTable('clothing_items', {
  id: text('id').primaryKey(),
  userId: text('user_id').notNull().references(() => users.id, { onDelete: 'cascade' }),
  name: text('name').notNull(),
  category: text('category').notNull(), // tops, bottoms, outerwear, footwear, accessories, one_piece, undergarments
  subcategory: text('subcategory'),
  brand: text('brand'),
  size: text('size'),
  colors: text('colors').notNull().default('[]'), // JSON array of strings
  season: text('season').notNull().default('all_season'),
  tags: text('tags').notNull().default('[]'), // JSON array of strings
  imageUrl: text('image_url'),
  thumbnailUrl: text('thumbnail_url'),
  hasTransparentBg: integer('has_transparent_bg', { mode: 'boolean' }).notNull().default(false),
  wearsSinceWash: integer('wears_since_wash').notNull().default(0),
  maxWearsBeforeWash: integer('max_wears_before_wash').notNull().default(2),
  totalLifetimeWears: integer('total_lifetime_wears').notNull().default(0),
  totalWashes: integer('total_washes').notNull().default(0),
  status: text('status').notNull().default('clean'), // clean, needs_wash, in_laundry, dry_cleaning
  minTempC: real('min_temp_c'),
  maxTempC: real('max_temp_c'),
  rainFriendly: integer('rain_friendly', { mode: 'boolean' }).notNull().default(false),
  notes: text('notes'),
  lastWornAt: text('last_worn_at'),
  createdAt: text('created_at').notNull(),
  updatedAt: text('updated_at').notNull()
});

export const outfits = sqliteTable('outfits', {
  id: text('id').primaryKey(),
  userId: text('user_id').notNull().references(() => users.id, { onDelete: 'cascade' }),
  name: text('name').notNull(),
  occasion: text('occasion').notNull().default('casual'),
  season: text('season').notNull().default('all_season'),
  isPublic: integer('is_public', { mode: 'boolean' }).notNull().default(false),
  itemIds: text('item_ids').notNull().default('[]'), // JSON array of clothing_item ids
  wearCount: integer('wear_count').notNull().default(0),
  lastWornAt: text('last_worn_at'),
  createdAt: text('created_at').notNull(),
  updatedAt: text('updated_at').notNull()
});

export const wearLogs = sqliteTable('wear_logs', {
  id: text('id').primaryKey(),
  userId: text('user_id').notNull().references(() => users.id, { onDelete: 'cascade' }),
  outfitId: text('outfit_id').references(() => outfits.id, { onDelete: 'set null' }),
  itemIds: text('item_ids').notNull().default('[]'), // JSON array of clothing_item ids
  date: text('date').notNull(), // YYYY-MM-DD
  status: text('status').notNull().default('planned'), // planned, worn, skipped
  notes: text('notes'),
  confirmedAt: text('confirmed_at'),
  createdAt: text('created_at').notNull(),
  updatedAt: text('updated_at').notNull()
});

export const ratings = sqliteTable('ratings', {
  id: text('id').primaryKey(),
  outfitId: text('outfit_id').notNull().references(() => outfits.id, { onDelete: 'cascade' }),
  userId: text('user_id').notNull().references(() => users.id, { onDelete: 'cascade' }),
  raterType: text('rater_type').notNull(), // self, partner, friend
  raterName: text('rater_name').notNull(),
  stars: integer('stars').notNull(), // 1 to 5
  feedback: text('feedback'),
  createdAt: text('created_at').notNull(),
  updatedAt: text('updated_at').notNull()
});

export const comments = sqliteTable('comments', {
  id: text('id').primaryKey(),
  outfitId: text('outfit_id').notNull().references(() => outfits.id, { onDelete: 'cascade' }),
  userId: text('user_id').notNull().references(() => users.id, { onDelete: 'cascade' }),
  userName: text('user_name').notNull(),
  content: text('content').notNull(),
  createdAt: text('created_at').notNull(),
  updatedAt: text('updated_at').notNull()
});

export const weatherCache = sqliteTable('weather_cache', {
  id: text('id').primaryKey(), // lat_lon_YYYY-MM-DD
  city: text('city'),
  lat: real('lat').notNull(),
  lon: real('lon').notNull(),
  date: text('date').notNull(),
  tempCurrentC: real('temp_current_c').notNull(),
  tempHighC: real('temp_high_c').notNull(),
  tempLowC: real('temp_low_c').notNull(),
  precipitationProbability: real('precipitation_probability').notNull(),
  weatherCode: integer('weather_code').notNull(),
  description: text('description').notNull(),
  isRaining: integer('is_raining', { mode: 'boolean' }).notNull(),
  fetchedAt: text('fetched_at').notNull()
});
