import { describe, it, expect, beforeAll } from 'vitest';
import { runMigrations } from './db/migrate.js';
import { AuthService } from './services/auth.js';
import { ClothingItemService } from './services/items.js';
import { OutfitService } from './services/outfits.js';
import { WearLogService } from './services/wear-logs.js';
import { SocialService } from './services/social.js';
import { RecommendationService } from './services/recommendations.js';
import { BackupService } from './services/backup.js';

describe('Trousseau Core Engine Tests', () => {
  let userId: string;
  let topItemId: string;
  let bottomItemId: string;
  let jacketItemId: string;
  let outfitId: string;

  beforeAll(() => {
    runMigrations();
  });

  it('1. Registers and authenticates a user', async () => {
    const testUsername = `testuser_${Date.now()}`;
    const user = await AuthService.register({
      username: testUsername,
      displayName: 'Wardrobe Tester',
      email: `${testUsername}@example.com`,
      password: 'SecurePassword123!',
      locationCity: 'New York',
      locationLat: 40.71,
      locationLon: -74.00
    });

    expect(user.id).toBeDefined();
    expect(user.username).toBe(testUsername);
    userId = user.id;

    // Login test
    const loginUser = await AuthService.login({
      usernameOrEmail: testUsername,
      password: 'SecurePassword123!'
    });
    expect(loginUser.id).toBe(userId);
  });

  it('2. Catalogs clothing items with automatic wash threshold defaults', async () => {
    // White Oxford Shirt (tops default to 2 max wears)
    const shirt = await ClothingItemService.create(userId, {
      name: 'White Oxford Shirt',
      category: 'tops',
      brand: 'Uniqlo',
      colors: ['#FFFFFF'],
      season: 'all_season',
      tags: ['office', 'casual'],
      maxWearsBeforeWash: 2
    });
    expect(shirt.maxWearsBeforeWash).toBe(2);
    expect(shirt.wearsSinceWash).toBe(0);
    expect(shirt.status).toBe('clean');
    topItemId = shirt.id;

    // Raw Denim Jeans (max 4 wears)
    const jeans = await ClothingItemService.create(userId, {
      name: 'Selvedge Denim Jeans',
      category: 'bottoms',
      brand: 'Naked & Famous',
      colors: ['#1C39BB'],
      season: 'all_season',
      maxWearsBeforeWash: 4
    });
    bottomItemId = jeans.id;

    // Wool Overcoat (cold weather & rain friendly)
    const coat = await ClothingItemService.create(userId, {
      name: 'Charcoal Wool Overcoat',
      category: 'outerwear',
      colors: ['#36454F'],
      season: 'winter',
      minTempC: 0,
      maxTempC: 12,
      rainFriendly: true,
      maxWearsBeforeWash: 15
    });
    jacketItemId = coat.id;

    const list = await ClothingItemService.list(userId);
    expect(list.length).toBeGreaterThanOrEqual(3);
  });

  it('3. Combines items into an Outfit', async () => {
    const outfit = await OutfitService.create(userId, {
      name: 'Classic Casual Uniform',
      occasion: 'casual',
      season: 'all_season',
      itemIds: [topItemId, bottomItemId, jacketItemId]
    });

    expect(outfit.id).toBeDefined();
    expect(outfit.items?.length).toBe(3);
    outfitId = outfit.id;
  });

  it('4. Tracks daily wears and automatically flags laundry when threshold is met', async () => {
    const today = new Date().toISOString().split('T')[0];

    // Day 1 wear
    const log1 = await WearLogService.create(userId, {
      outfitId,
      itemIds: [topItemId, bottomItemId, jacketItemId],
      date: today,
      status: 'worn'
    });
    expect(log1.status).toBe('worn');

    let shirt = await ClothingItemService.getById(userId, topItemId);
    expect(shirt?.wearsSinceWash).toBe(1);
    expect(shirt?.status).toBe('clean'); // 1 < 2, still clean

    // Day 2 wear
    await WearLogService.create(userId, {
      outfitId,
      itemIds: [topItemId, bottomItemId, jacketItemId],
      date: '2026-11-08',
      status: 'worn'
    });

    shirt = await ClothingItemService.getById(userId, topItemId);
    expect(shirt?.wearsSinceWash).toBe(2);
    // Max wears reached! Must automatically flag needs_wash
    expect(shirt?.status).toBe('needs_wash');
  });

  it('5. Excludes dirty outfits from active recommendations (Cleanliness Gate)', async () => {
    const recs = await RecommendationService.getRecommendations(userId);
    // Since the shirt in our outfit has status 'needs_wash', the outfit should NOT be recommended
    const foundDirty = recs.find(r => r.outfit.id === outfitId);
    expect(foundDirty).toBeUndefined();
  });

  it('6. Washes item, resets wears counter, and restores recommendation eligibility', async () => {
    await ClothingItemService.washItem(userId, topItemId);
    const shirt = await ClothingItemService.getById(userId, topItemId);
    expect(shirt?.wearsSinceWash).toBe(0);
    expect(shirt?.status).toBe('clean');
    expect(shirt?.totalWashes).toBe(1);

    // Now with clean shirt, outfit can be recommended again
    const recs = await RecommendationService.getRecommendations(userId);
    const foundClean = recs.find(r => r.outfit.id === outfitId);
    expect(foundClean).toBeDefined();
  });

  it('7. Supports multi-perspective ratings and comments', async () => {
    // Self rating
    await SocialService.addRating(userId, outfitId, {
      raterType: 'self',
      raterName: 'Self',
      stars: 5,
      feedback: 'Extremely comfortable'
    });

    // Partner rating
    await SocialService.addRating(userId, outfitId, {
      raterType: 'partner',
      raterName: 'Alex',
      stars: 4,
      feedback: 'Looks very sharp'
    });

    // Friend rating
    await SocialService.addRating(userId, outfitId, {
      raterType: 'friend',
      raterName: 'Sam',
      stars: 5
    });

    const outfit = await OutfitService.getById(userId, outfitId);
    expect(outfit?.avgRating).toBe(4.7);
    expect(outfit?.ratingCounts?.self).toBe(1);
    expect(outfit?.ratingCounts?.partner).toBe(1);
    expect(outfit?.ratingCounts?.friend).toBe(1);

    // Add comment
    const comment = await SocialService.addComment(userId, 'Tester', outfitId, {
      content: 'Love this combo with boots!'
    });
    expect(comment.id).toBeDefined();
    const commentsList = await SocialService.listComments(outfitId);
    expect(commentsList.length).toBeGreaterThanOrEqual(1);
  });

  it('8. Exports and restores complete standalone ZIP backup', async () => {
    const zipBuffer = await BackupService.exportUserData(userId);
    expect(zipBuffer).toBeInstanceOf(Buffer);
    expect(zipBuffer.length).toBeGreaterThan(100);

    // Verify import
    const importResult = await BackupService.importUserData(userId, zipBuffer);
    expect(importResult.restoredItems).toBeGreaterThanOrEqual(3);
    expect(importResult.restoredOutfits).toBeGreaterThanOrEqual(1);
  });
});
