import AdmZip from 'adm-zip';
import path from 'node:path';
import fs from 'node:fs';
import { eq } from 'drizzle-orm';
import { db, storagePaths } from '../db/index.js';
import {
  users,
  clothingItems,
  outfits,
  wearLogs,
  ratings,
  comments
} from '../db/schema.js';
import {
  type ExportManifest,
  ExportManifestSchema
} from '@trousseau/shared';
import { AuthService } from './auth.js';
import { ClothingItemService } from './items.js';
import { OutfitService } from './outfits.js';
import { WearLogService } from './wear-logs.js';
import { SocialService } from './social.js';

export class BackupService {
  /**
   * Generates a complete standalone ZIP backup of all user data and photos.
   */
  static async exportUserData(userId: string): Promise<Buffer> {
    const user = await AuthService.getUserById(userId);
    if (!user) throw new Error('User not found');

    const items = await ClothingItemService.list(userId);
    const userOutfits = await OutfitService.list(userId);
    const logs = await WearLogService.list(userId, 1000);

    const userRatings = db.select().from(ratings).where(eq(ratings.userId, userId)).all().map(SocialService.formatRating);
    const userComments = db.select().from(comments).where(eq(comments.userId, userId)).all().map(SocialService.formatComment);

    const manifest: ExportManifest = {
      version: '1.0.0',
      exportedAt: new Date().toISOString(),
      appName: 'Trousseau',
      user,
      items,
      outfits: userOutfits,
      wearLogs: logs,
      ratings: userRatings,
      comments: userComments
    };

    const zip = new AdmZip();

    // 1. Add manifest
    zip.addFile('manifest.json', Buffer.from(JSON.stringify(manifest, null, 2), 'utf-8'));

    // 2. Add clothing image files
    for (const item of items) {
      if (item.imageUrl) {
        const imgName = path.basename(item.imageUrl);
        const fullImgPath = path.join(storagePaths.imagesDir, imgName);
        if (fs.existsSync(fullImgPath)) {
          zip.addLocalFile(fullImgPath, 'images');
        }
      }
      if (item.thumbnailUrl) {
        const thumbName = path.basename(item.thumbnailUrl);
        const fullThumbPath = path.join(storagePaths.thumbnailsDir, thumbName);
        if (fs.existsSync(fullThumbPath)) {
          zip.addLocalFile(fullThumbPath, 'thumbnails');
        }
      }
    }

    return zip.toBuffer();
  }

  /**
   * Restores user data and photos from an uploaded ZIP backup.
   */
  static async importUserData(userId: string, zipBuffer: Buffer): Promise<{ restoredItems: number; restoredOutfits: number }> {
    const zip = new AdmZip(zipBuffer);
    const manifestEntry = zip.getEntry('manifest.json');
    if (!manifestEntry) {
      throw new Error('Invalid backup archive: missing manifest.json');
    }

    const manifestJson = JSON.parse(zip.readAsText(manifestEntry));
    const validatedManifest = ExportManifestSchema.parse(manifestJson);

    // Extract photos to disk
    const zipEntries = zip.getEntries();
    for (const entry of zipEntries) {
      if (entry.isDirectory) continue;

      if (entry.entryName.startsWith('images/')) {
        const filename = path.basename(entry.entryName);
        const dest = path.join(storagePaths.imagesDir, filename);
        fs.writeFileSync(dest, entry.getData());
      } else if (entry.entryName.startsWith('thumbnails/')) {
        const filename = path.basename(entry.entryName);
        const dest = path.join(storagePaths.thumbnailsDir, filename);
        fs.writeFileSync(dest, entry.getData());
      }
    }

    // Restore Clothing Items
    let restoredItems = 0;
    for (const item of validatedManifest.items) {
      const existing = db.select().from(clothingItems).where(eq(clothingItems.id, item.id)).get();
      const row = {
        id: item.id,
        userId,
        name: item.name,
        category: item.category,
        subcategory: item.subcategory ?? null,
        brand: item.brand ?? null,
        size: item.size ?? null,
        colors: JSON.stringify(item.colors ?? []),
        season: item.season ?? 'all_season',
        tags: JSON.stringify(item.tags ?? []),
        imageUrl: item.imageUrl ?? null,
        thumbnailUrl: item.thumbnailUrl ?? null,
        hasTransparentBg: item.hasTransparentBg ? 1 : 0,
        wearsSinceWash: item.wearsSinceWash,
        maxWearsBeforeWash: item.maxWearsBeforeWash,
        totalLifetimeWears: item.totalLifetimeWears,
        totalWashes: item.totalWashes,
        status: item.status,
        minTempC: item.minTempC ?? null,
        maxTempC: item.maxTempC ?? null,
        rainFriendly: item.rainFriendly ? 1 : 0,
        notes: item.notes ?? null,
        lastWornAt: item.lastWornAt ?? null,
        createdAt: item.createdAt,
        updatedAt: item.updatedAt
      };

      if (existing) {
        db.update(clothingItems).set(row as any).where(eq(clothingItems.id, item.id)).run();
      } else {
        db.insert(clothingItems).values(row as any).run();
      }
      restoredItems++;
    }

    // Restore Outfits
    let restoredOutfits = 0;
    for (const outfit of validatedManifest.outfits) {
      const existing = db.select().from(outfits).where(eq(outfits.id, outfit.id)).get();
      const row = {
        id: outfit.id,
        userId,
        name: outfit.name,
        occasion: outfit.occasion,
        season: outfit.season,
        isPublic: outfit.isPublic ? 1 : 0,
        itemIds: JSON.stringify(outfit.itemIds),
        wearCount: outfit.wearCount,
        lastWornAt: outfit.lastWornAt ?? null,
        createdAt: outfit.createdAt,
        updatedAt: outfit.updatedAt
      };

      if (existing) {
        db.update(outfits).set(row as any).where(eq(outfits.id, outfit.id)).run();
      } else {
        db.insert(outfits).values(row as any).run();
      }
      restoredOutfits++;
    }

    return { restoredItems, restoredOutfits };
  }
}
