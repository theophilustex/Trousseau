import { eq, and, sql } from 'drizzle-orm';
import { nanoid } from 'nanoid';
import { db } from '../db/index.js';
import { outfits, ratings } from '../db/schema.js';
import {
  type Outfit,
  type CreateOutfitInput,
  type UpdateOutfitInput,
  type Occasion,
  type Season
} from '@trousseau/shared';
import { ClothingItemService } from './items.js';

export class OutfitService {
  static async populateOutfit(userId: string, row: typeof outfits.$inferSelect): Promise<Outfit> {
    const itemIds: string[] = JSON.parse(row.itemIds || '[]');
    const items = await ClothingItemService.getByIds(userId, itemIds);

    // Calculate rating summary
    const ratingRows = db.select()
      .from(ratings)
      .where(eq(ratings.outfitId, row.id))
      .all();

    let avgRating: number | null = null;
    const ratingCounts = { self: 0, partner: 0, friend: 0 };

    if (ratingRows.length > 0) {
      const sum = ratingRows.reduce((acc, r) => acc + r.stars, 0);
      avgRating = Number((sum / ratingRows.length).toFixed(1));

      for (const r of ratingRows) {
        if (r.raterType === 'self') ratingCounts.self++;
        else if (r.raterType === 'partner') ratingCounts.partner++;
        else if (r.raterType === 'friend') ratingCounts.friend++;
      }
    }

    return {
      id: row.id,
      userId: row.userId,
      name: row.name,
      occasion: row.occasion as Occasion,
      season: row.season as Season,
      isPublic: Boolean(row.isPublic),
      itemIds,
      items,
      avgRating,
      ratingCounts,
      lastWornAt: row.lastWornAt,
      wearCount: row.wearCount,
      createdAt: row.createdAt,
      updatedAt: row.updatedAt
    };
  }

  static async list(userId: string, filter?: { occasion?: Occasion; season?: Season }): Promise<Outfit[]> {
    let rows = db.select().from(outfits).where(eq(outfits.userId, userId)).all();

    if (filter?.occasion) {
      rows = rows.filter(r => r.occasion === filter.occasion);
    }
    if (filter?.season && filter.season !== 'all_season') {
      rows = rows.filter(r => r.season === filter.season || r.season === 'all_season');
    }

    return Promise.all(rows.map(r => this.populateOutfit(userId, r)));
  }

  static async listPublic(): Promise<Outfit[]> {
    const rows = db.select().from(outfits).where(eq(outfits.isPublic, true)).all();
    return Promise.all(rows.map(r => this.populateOutfit(r.userId, r)));
  }

  static async getById(userId: string, id: string): Promise<Outfit | null> {
    const row = db.select()
      .from(outfits)
      .where(and(eq(outfits.id, id), eq(outfits.userId, userId)))
      .get();

    return row ? this.populateOutfit(userId, row) : null;
  }

  static async create(userId: string, input: CreateOutfitInput): Promise<Outfit> {
    const id = nanoid();
    const now = new Date().toISOString();

    const newRow = {
      id,
      userId,
      name: input.name,
      occasion: input.occasion,
      season: input.season,
      isPublic: input.isPublic ? 1 : 0,
      itemIds: JSON.stringify(input.itemIds),
      wearCount: 0,
      lastWornAt: null,
      createdAt: now,
      updatedAt: now
    };

    db.insert(outfits).values(newRow as any).run();
    const created = await this.getById(userId, id);
    if (!created) throw new Error('Failed to create outfit');
    return created;
  }

  static async update(userId: string, id: string, input: UpdateOutfitInput): Promise<Outfit> {
    const existing = await this.getById(userId, id);
    if (!existing) throw new Error('Outfit not found');

    const now = new Date().toISOString();
    const updateData: any = { updatedAt: now };

    if (input.name !== undefined) updateData.name = input.name;
    if (input.occasion !== undefined) updateData.occasion = input.occasion;
    if (input.season !== undefined) updateData.season = input.season;
    if (input.isPublic !== undefined) updateData.isPublic = input.isPublic ? 1 : 0;
    if (input.itemIds !== undefined) updateData.itemIds = JSON.stringify(input.itemIds);

    db.update(outfits)
      .set(updateData)
      .where(and(eq(outfits.id, id), eq(outfits.userId, userId)))
      .run();

    const updated = await this.getById(userId, id);
    if (!updated) throw new Error('Failed to update outfit');
    return updated;
  }

  static async delete(userId: string, id: string): Promise<boolean> {
    const res = db.delete(outfits)
      .where(and(eq(outfits.id, id), eq(outfits.userId, userId)))
      .run();

    return res.changes > 0;
  }
}
