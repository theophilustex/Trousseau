import { eq, and, like, or } from 'drizzle-orm';
import { nanoid } from 'nanoid';
import { db } from '../db/index.js';
import { clothingItems } from '../db/schema.js';
import {
  type ClothingItem,
  type CreateClothingItemInput,
  type UpdateClothingItemInput,
  type ClothingCategory,
  type WashStatus,
  DEFAULT_MAX_WEARS
} from '@trousseau/shared';
import { MediaService } from './media.js';

export interface ItemFilterOptions {
  category?: ClothingCategory;
  status?: WashStatus;
  season?: string;
  tag?: string;
  search?: string;
  cleanOnly?: boolean;
}

export class ClothingItemService {
  static formatItem(row: typeof clothingItems.$inferSelect): ClothingItem {
    return {
      id: row.id,
      userId: row.userId,
      name: row.name,
      category: row.category as any,
      subcategory: row.subcategory,
      brand: row.brand,
      size: row.size,
      colors: JSON.parse(row.colors || '[]'),
      season: row.season as any,
      tags: JSON.parse(row.tags || '[]'),
      imageUrl: row.imageUrl,
      thumbnailUrl: row.thumbnailUrl,
      hasTransparentBg: Boolean(row.hasTransparentBg),
      wearsSinceWash: row.wearsSinceWash,
      maxWearsBeforeWash: row.maxWearsBeforeWash,
      totalLifetimeWears: row.totalLifetimeWears,
      totalWashes: row.totalWashes,
      status: row.status as any,
      minTempC: row.minTempC,
      maxTempC: row.maxTempC,
      rainFriendly: Boolean(row.rainFriendly),
      notes: row.notes,
      lastWornAt: row.lastWornAt,
      createdAt: row.createdAt,
      updatedAt: row.updatedAt
    };
  }

  static async list(userId: string, filters: ItemFilterOptions = {}): Promise<ClothingItem[]> {
    let rows = db.select().from(clothingItems).where(eq(clothingItems.userId, userId)).all();

    if (filters.category) {
      rows = rows.filter(r => r.category === filters.category);
    }
    if (filters.status) {
      rows = rows.filter(r => r.status === filters.status);
    }
    if (filters.cleanOnly) {
      rows = rows.filter(r => r.status === 'clean');
    }
    if (filters.season && filters.season !== 'all_season') {
      rows = rows.filter(r => r.season === filters.season || r.season === 'all_season');
    }
    if (filters.tag) {
      rows = rows.filter(r => {
        const tags: string[] = JSON.parse(r.tags || '[]');
        return tags.includes(filters.tag!);
      });
    }
    if (filters.search) {
      const q = filters.search.toLowerCase();
      rows = rows.filter(r =>
        r.name.toLowerCase().includes(q) ||
        (r.brand && r.brand.toLowerCase().includes(q)) ||
        (r.subcategory && r.subcategory.toLowerCase().includes(q))
      );
    }

    return rows.map(this.formatItem);
  }

  static async getById(userId: string, id: string): Promise<ClothingItem | null> {
    const row = db.select()
      .from(clothingItems)
      .where(and(eq(clothingItems.id, id), eq(clothingItems.userId, userId)))
      .get();

    return row ? this.formatItem(row) : null;
  }

  static async getByIds(userId: string, ids: string[]): Promise<ClothingItem[]> {
    if (ids.length === 0) return [];
    const all = await this.list(userId);
    const idSet = new Set(ids);
    return all.filter(item => idSet.has(item.id));
  }

  static async create(userId: string, input: CreateClothingItemInput): Promise<ClothingItem> {
    const id = nanoid();
    const now = new Date().toISOString();
    const maxWears = input.maxWearsBeforeWash ?? DEFAULT_MAX_WEARS[input.category] ?? 2;

    const newRow = {
      id,
      userId,
      name: input.name,
      category: input.category,
      subcategory: input.subcategory ?? null,
      brand: input.brand ?? null,
      size: input.size ?? null,
      colors: JSON.stringify(input.colors ?? []),
      season: input.season ?? 'all_season',
      tags: JSON.stringify(input.tags ?? []),
      imageUrl: input.imageUrl ?? null,
      thumbnailUrl: input.thumbnailUrl ?? null,
      hasTransparentBg: input.hasTransparentBg ? 1 : 0,
      wearsSinceWash: 0,
      maxWearsBeforeWash: maxWears,
      totalLifetimeWears: 0,
      totalWashes: 0,
      status: 'clean',
      minTempC: input.minTempC ?? null,
      maxTempC: input.maxTempC ?? null,
      rainFriendly: input.rainFriendly ? 1 : 0,
      notes: input.notes ?? null,
      lastWornAt: null,
      createdAt: now,
      updatedAt: now
    };

    db.insert(clothingItems).values(newRow as any).run();
    const item = await this.getById(userId, id);
    if (!item) throw new Error('Failed to create clothing item');
    return item;
  }

  static async update(userId: string, id: string, input: UpdateClothingItemInput): Promise<ClothingItem> {
    const existing = await this.getById(userId, id);
    if (!existing) throw new Error('Clothing item not found');

    const now = new Date().toISOString();
    const updateData: any = { updatedAt: now };

    if (input.name !== undefined) updateData.name = input.name;
    if (input.category !== undefined) updateData.category = input.category;
    if (input.subcategory !== undefined) updateData.subcategory = input.subcategory ?? null;
    if (input.brand !== undefined) updateData.brand = input.brand ?? null;
    if (input.size !== undefined) updateData.size = input.size ?? null;
    if (input.colors !== undefined) updateData.colors = JSON.stringify(input.colors);
    if (input.season !== undefined) updateData.season = input.season;
    if (input.tags !== undefined) updateData.tags = JSON.stringify(input.tags);
    if (input.imageUrl !== undefined) updateData.imageUrl = input.imageUrl ?? null;
    if (input.thumbnailUrl !== undefined) updateData.thumbnailUrl = input.thumbnailUrl ?? null;
    if (input.hasTransparentBg !== undefined) updateData.hasTransparentBg = input.hasTransparentBg ? 1 : 0;
    if (input.maxWearsBeforeWash !== undefined) updateData.maxWearsBeforeWash = input.maxWearsBeforeWash;
    if (input.minTempC !== undefined) updateData.minTempC = input.minTempC ?? null;
    if (input.maxTempC !== undefined) updateData.maxTempC = input.maxTempC ?? null;
    if (input.rainFriendly !== undefined) updateData.rainFriendly = input.rainFriendly ? 1 : 0;
    if (input.notes !== undefined) updateData.notes = input.notes ?? null;
    if (input.status !== undefined) updateData.status = input.status;
    if (input.wearsSinceWash !== undefined) {
      updateData.wearsSinceWash = input.wearsSinceWash;
      const threshold = input.maxWearsBeforeWash ?? existing.maxWearsBeforeWash;
      if (input.wearsSinceWash >= threshold && existing.status === 'clean') {
        updateData.status = 'needs_wash';
      }
    }

    db.update(clothingItems)
      .set(updateData)
      .where(and(eq(clothingItems.id, id), eq(clothingItems.userId, userId)))
      .run();

    const updated = await this.getById(userId, id);
    if (!updated) throw new Error('Item update failed');
    return updated;
  }

  static async delete(userId: string, id: string): Promise<boolean> {
    const existing = await this.getById(userId, id);
    if (!existing) return false;

    // Delete image files from disk
    MediaService.deleteFiles(existing.imageUrl, existing.thumbnailUrl);

    db.delete(clothingItems)
      .where(and(eq(clothingItems.id, id), eq(clothingItems.userId, userId)))
      .run();

    return true;
  }

  /**
   * Action: Mark item as washed and reset wears counter
   */
  static async washItem(userId: string, id: string): Promise<ClothingItem> {
    const item = await this.getById(userId, id);
    if (!item) throw new Error('Clothing item not found');

    const now = new Date().toISOString();
    db.update(clothingItems)
      .set({
        wearsSinceWash: 0,
        status: 'clean',
        totalWashes: item.totalWashes + 1,
        updatedAt: now
      })
      .where(and(eq(clothingItems.id, id), eq(clothingItems.userId, userId)))
      .run();

    return (await this.getById(userId, id))!;
  }

  /**
   * Action: Wash all items currently needing wash
   */
  static async washAllDirty(userId: string): Promise<number> {
    const dirtyItems = await this.list(userId, { status: 'needs_wash' });
    const inLaundry = await this.list(userId, { status: 'in_laundry' });
    const toWash = [...dirtyItems, ...inLaundry];

    for (const item of toWash) {
      await this.washItem(userId, item.id);
    }

    return toWash.length;
  }
}
