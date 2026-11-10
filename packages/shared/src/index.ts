import { z } from 'zod';

// ==========================================
// 1. Enums and Constants
// ==========================================

export const ClothingCategories = [
  'tops',
  'bottoms',
  'outerwear',
  'footwear',
  'accessories',
  'one_piece',
  'undergarments'
] as const;
export type ClothingCategory = typeof ClothingCategories[number];

export const Occasions = [
  'casual',
  'business',
  'formal',
  'date_night',
  'sport',
  'loungewear',
  'party'
] as const;
export type Occasion = typeof Occasions[number];

export const Seasons = [
  'spring',
  'summer',
  'fall',
  'winter',
  'all_season'
] as const;
export type Season = typeof Seasons[number];

export const WashStatuses = [
  'clean',
  'needs_wash',
  'in_laundry',
  'dry_cleaning'
] as const;
export type WashStatus = typeof WashStatuses[number];

export const RaterTypes = [
  'self',
  'partner',
  'friend'
] as const;
export type RaterType = typeof RaterTypes[number];

export const WearLogStatuses = [
  'planned',
  'worn',
  'skipped'
] as const;
export type WearLogStatus = typeof WearLogStatuses[number];

// Temperature helpers
export type TemperatureUnit = 'C' | 'F';

export function celsiusToFahrenheit(celsius: number): number {
  return Math.round((celsius * 9) / 5 + 32);
}

export function formatTemp(tempC: number | null | undefined, unit: TemperatureUnit = 'C'): string {
  if (tempC === null || tempC === undefined) return '--';
  if (unit === 'F') {
    return `${celsiusToFahrenheit(tempC)}°F`;
  }
  return `${Math.round(tempC)}°C`;
}

// Common default wash thresholds
export const DEFAULT_MAX_WEARS: Record<ClothingCategory, number> = {
  tops: 2,
  bottoms: 4,
  outerwear: 15,
  footwear: 30,
  accessories: 20,
  one_piece: 2,
  undergarments: 1
};

// ==========================================
// 2. User & Auth Schemas
// ==========================================

export const UserSchema = z.object({
  id: z.string(),
  username: z.string().min(3).max(30),
  displayName: z.string().min(1).max(50),
  email: z.string().email(),
  locationCity: z.string().optional().nullable(),
  locationLat: z.number().optional().nullable(),
  locationLon: z.number().optional().nullable(),
  createdAt: z.string(),
  updatedAt: z.string()
});
export type User = z.infer<typeof UserSchema>;

export const RegisterInputSchema = z.object({
  username: z.string().min(3).max(30),
  displayName: z.string().min(1).max(50),
  email: z.string().email(),
  password: z.string().min(8),
  locationCity: z.string().optional(),
  locationLat: z.number().optional(),
  locationLon: z.number().optional()
});
export type RegisterInput = z.infer<typeof RegisterInputSchema>;

export const LoginInputSchema = z.object({
  usernameOrEmail: z.string().min(1),
  password: z.string().min(1)
});
export type LoginInput = z.infer<typeof LoginInputSchema>;

export const AuthResponseSchema = z.object({
  token: z.string(),
  user: UserSchema
});
export type AuthResponse = z.infer<typeof AuthResponseSchema>;

// ==========================================
// 3. Clothing Item Schemas
// ==========================================

export const ClothingItemSchema = z.object({
  id: z.string(),
  userId: z.string(),
  name: z.string().min(1).max(100),
  category: z.enum(ClothingCategories),
  subcategory: z.string().optional().nullable(),
  brand: z.string().optional().nullable(),
  size: z.string().optional().nullable(),
  colors: z.array(z.string()).default([]),
  season: z.enum(Seasons).default('all_season'),
  tags: z.array(z.string()).default([]),
  imageUrl: z.string().optional().nullable(),
  thumbnailUrl: z.string().optional().nullable(),
  hasTransparentBg: z.boolean().default(false),
  wearsSinceWash: z.number().int().nonnegative().default(0),
  maxWearsBeforeWash: z.number().int().positive().default(2),
  totalLifetimeWears: z.number().int().nonnegative().default(0),
  totalWashes: z.number().int().nonnegative().default(0),
  status: z.enum(WashStatuses).default('clean'),
  minTempC: z.number().optional().nullable(),
  maxTempC: z.number().optional().nullable(),
  rainFriendly: z.boolean().default(false),
  notes: z.string().optional().nullable(),
  lastWornAt: z.string().optional().nullable(),
  createdAt: z.string(),
  updatedAt: z.string()
});
export type ClothingItem = z.infer<typeof ClothingItemSchema>;

export const CreateClothingItemSchema = z.object({
  name: z.string().min(1).max(100),
  category: z.enum(ClothingCategories),
  subcategory: z.string().optional(),
  brand: z.string().optional(),
  size: z.string().optional(),
  colors: z.array(z.string()).default([]),
  season: z.enum(Seasons).default('all_season'),
  tags: z.array(z.string()).default([]),
  imageUrl: z.string().optional(),
  thumbnailUrl: z.string().optional(),
  hasTransparentBg: z.boolean().default(false),
  maxWearsBeforeWash: z.number().int().positive().optional(),
  minTempC: z.number().optional(),
  maxTempC: z.number().optional(),
  rainFriendly: z.boolean().default(false),
  notes: z.string().optional()
});
export type CreateClothingItemInput = z.input<typeof CreateClothingItemSchema>;

export const UpdateClothingItemSchema = CreateClothingItemSchema.partial().extend({
  status: z.enum(WashStatuses).optional(),
  wearsSinceWash: z.number().int().nonnegative().optional()
});
export type UpdateClothingItemInput = z.input<typeof UpdateClothingItemSchema>;

// ==========================================
// 4. Outfit Schemas
// ==========================================

export const OutfitSchema = z.object({
  id: z.string(),
  userId: z.string(),
  name: z.string().min(1).max(100),
  occasion: z.enum(Occasions).default('casual'),
  season: z.enum(Seasons).default('all_season'),
  isPublic: z.boolean().default(false),
  itemIds: z.array(z.string()),
  items: z.array(ClothingItemSchema).optional(),
  avgRating: z.number().optional().nullable(),
  ratingCounts: z.object({
    self: z.number().default(0),
    partner: z.number().default(0),
    friend: z.number().default(0)
  }).optional(),
  lastWornAt: z.string().optional().nullable(),
  wearCount: z.number().int().nonnegative().default(0),
  createdAt: z.string(),
  updatedAt: z.string()
});
export type Outfit = z.infer<typeof OutfitSchema>;

export const CreateOutfitSchema = z.object({
  name: z.string().min(1).max(100),
  occasion: z.enum(Occasions).default('casual'),
  season: z.enum(Seasons).default('all_season'),
  isPublic: z.boolean().default(false),
  itemIds: z.array(z.string()).min(1, 'An outfit must have at least one clothing item')
});
export type CreateOutfitInput = z.infer<typeof CreateOutfitSchema>;

export const UpdateOutfitSchema = CreateOutfitSchema.partial();
export type UpdateOutfitInput = z.infer<typeof UpdateOutfitSchema>;

// ==========================================
// 5. Wear Log & Daily Tracking Schemas
// ==========================================

export const WearLogSchema = z.object({
  id: z.string(),
  userId: z.string(),
  outfitId: z.string().optional().nullable(),
  outfitName: z.string().optional().nullable(),
  itemIds: z.array(z.string()),
  date: z.string(), // YYYY-MM-DD
  status: z.enum(WearLogStatuses).default('planned'),
  notes: z.string().optional().nullable(),
  confirmedAt: z.string().optional().nullable(),
  createdAt: z.string(),
  updatedAt: z.string()
});
export type WearLog = z.infer<typeof WearLogSchema>;

export const CreateWearLogSchema = z.object({
  outfitId: z.string().optional(),
  itemIds: z.array(z.string()).min(1),
  date: z.string().regex(/^\d{4}-\d{2}-\d{2}$/, 'Date must be formatted as YYYY-MM-DD'),
  status: z.enum(WearLogStatuses).default('planned'),
  notes: z.string().optional()
});
export type CreateWearLogInput = z.infer<typeof CreateWearLogSchema>;

export const ConfirmWearLogSchema = z.object({
  status: z.enum(['worn', 'skipped']),
  replacementOutfitId: z.string().optional(),
  notes: z.string().optional()
});
export type ConfirmWearLogInput = z.infer<typeof ConfirmWearLogSchema>;

// ==========================================
// 6. Rating & Comment Schemas
// ==========================================

export const RatingSchema = z.object({
  id: z.string(),
  outfitId: z.string(),
  userId: z.string(),
  raterType: z.enum(RaterTypes),
  raterName: z.string().min(1).max(50),
  stars: z.number().int().min(1).max(5),
  feedback: z.string().max(500).optional().nullable(),
  createdAt: z.string(),
  updatedAt: z.string()
});
export type Rating = z.infer<typeof RatingSchema>;

export const CreateRatingSchema = z.object({
  raterType: z.enum(RaterTypes),
  raterName: z.string().min(1).max(50),
  stars: z.number().int().min(1).max(5),
  feedback: z.string().max(500).optional()
});
export type CreateRatingInput = z.infer<typeof CreateRatingSchema>;

export const CommentSchema = z.object({
  id: z.string(),
  outfitId: z.string(),
  userId: z.string(),
  userName: z.string(),
  content: z.string().min(1).max(1000),
  createdAt: z.string(),
  updatedAt: z.string()
});
export type Comment = z.infer<typeof CommentSchema>;

export const CreateCommentSchema = z.object({
  content: z.string().min(1).max(1000)
});
export type CreateCommentInput = z.infer<typeof CreateCommentSchema>;

// ==========================================
// 7. Weather & Recommendation Schemas
// ==========================================

export const WeatherForecastSchema = z.object({
  date: z.string(), // YYYY-MM-DD
  tempCurrentC: z.number(),
  tempHighC: z.number(),
  tempLowC: z.number(),
  precipitationProbability: z.number(), // 0 to 100
  weatherCode: z.number(),
  description: z.string(),
  isRaining: z.boolean(),
  city: z.string().optional()
});
export type WeatherForecast = z.infer<typeof WeatherForecastSchema>;

export const OutfitRecommendationSchema = z.object({
  outfit: OutfitSchema,
  score: z.number(),
  reason: z.string(),
  weatherScore: z.number(),
  recencyPenalty: z.number(),
  cleanlinessScore: z.number(),
  daysSinceLastWorn: z.number().nullable()
});
export type OutfitRecommendation = z.infer<typeof OutfitRecommendationSchema>;

// ==========================================
// 8. Backup & Export Schemas
// ==========================================

export const ExportManifestSchema = z.object({
  version: z.literal('1.0.0'),
  exportedAt: z.string(),
  appName: z.literal('Trousseau'),
  user: UserSchema,
  items: z.array(ClothingItemSchema),
  outfits: z.array(OutfitSchema),
  wearLogs: z.array(WearLogSchema),
  ratings: z.array(RatingSchema),
  comments: z.array(CommentSchema)
});
export type ExportManifest = z.infer<typeof ExportManifestSchema>;
