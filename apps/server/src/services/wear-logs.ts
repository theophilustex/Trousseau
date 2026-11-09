import { eq, and, desc } from 'drizzle-orm';
import { nanoid } from 'nanoid';
import { db } from '../db/index.js';
import { wearLogs, outfits, clothingItems } from '../db/schema.js';
import {
  type WearLog,
  type CreateWearLogInput,
  type ConfirmWearLogInput
} from '@trousseau/shared';
import { ClothingItemService } from './items.js';
import { OutfitService } from './outfits.js';

export class WearLogService {
  static formatRow(row: typeof wearLogs.$inferSelect, outfitName?: string): WearLog {
    return {
      id: row.id,
      userId: row.userId,
      outfitId: row.outfitId,
      outfitName: outfitName ?? null,
      itemIds: JSON.parse(row.itemIds || '[]'),
      date: row.date,
      status: row.status as any,
      notes: row.notes,
      confirmedAt: row.confirmedAt,
      createdAt: row.createdAt,
      updatedAt: row.updatedAt
    };
  }

  static async list(userId: string, limit = 50): Promise<WearLog[]> {
    const rows = db.select()
      .from(wearLogs)
      .where(eq(wearLogs.userId, userId))
      .orderBy(desc(wearLogs.date))
      .limit(limit)
      .all();

    return Promise.all(rows.map(async r => {
      let outfitName: string | undefined;
      if (r.outfitId) {
        const outfit = await OutfitService.getById(userId, r.outfitId);
        outfitName = outfit?.name;
      }
      return this.formatRow(r, outfitName);
    }));
  }

  static async getById(userId: string, id: string): Promise<WearLog | null> {
    const row = db.select()
      .from(wearLogs)
      .where(and(eq(wearLogs.id, id), eq(wearLogs.userId, userId)))
      .get();

    if (!row) return null;
    let outfitName: string | undefined;
    if (row.outfitId) {
      const outfit = await OutfitService.getById(userId, row.outfitId);
      outfitName = outfit?.name;
    }
    return this.formatRow(row, outfitName);
  }

  static async getByDate(userId: string, date: string): Promise<WearLog | null> {
    const row = db.select()
      .from(wearLogs)
      .where(and(eq(wearLogs.userId, userId), eq(wearLogs.date, date)))
      .get();

    if (!row) return null;
    let outfitName: string | undefined;
    if (row.outfitId) {
      const outfit = await OutfitService.getById(userId, row.outfitId);
      outfitName = outfit?.name;
    }
    return this.formatRow(row, outfitName);
  }

  /**
   * Plan or log an outfit for a given date (defaults to today)
   */
  static async create(userId: string, input: CreateWearLogInput): Promise<WearLog> {
    const existing = await this.getByDate(userId, input.date);
    if (existing) {
      // Update existing plan for this date
      return this.update(userId, existing.id, input);
    }

    const id = nanoid();
    const now = new Date().toISOString();

    const newRow = {
      id,
      userId,
      outfitId: input.outfitId ?? null,
      itemIds: JSON.stringify(input.itemIds),
      date: input.date,
      status: input.status,
      notes: input.notes ?? null,
      confirmedAt: input.status === 'worn' ? now : null,
      createdAt: now,
      updatedAt: now
    };

    db.insert(wearLogs).values(newRow as any).run();

    // If directly marked as worn, update wear counts immediately
    if (input.status === 'worn') {
      await this.applyWears(userId, input.outfitId, input.itemIds);
    }

    return (await this.getById(userId, id))!;
  }

  static async update(userId: string, id: string, input: Partial<CreateWearLogInput>): Promise<WearLog> {
    const existing = await this.getById(userId, id);
    if (!existing) throw new Error('Wear log not found');

    const now = new Date().toISOString();
    const updateData: any = { updatedAt: now };

    if (input.outfitId !== undefined) updateData.outfitId = input.outfitId;
    if (input.itemIds !== undefined) updateData.itemIds = JSON.stringify(input.itemIds);
    if (input.status !== undefined) updateData.status = input.status;
    if (input.notes !== undefined) updateData.notes = input.notes;

    db.update(wearLogs)
      .set(updateData)
      .where(and(eq(wearLogs.id, id), eq(wearLogs.userId, userId)))
      .run();

    if (input.status === 'worn' && existing.status !== 'worn') {
      const finalOutfitId = input.outfitId ?? existing.outfitId ?? undefined;
      const finalItemIds = input.itemIds ?? existing.itemIds;
      await this.applyWears(userId, finalOutfitId, finalItemIds);
    }

    return (await this.getById(userId, id))!;
  }

  /**
   * Mobile check-in: Confirm whether user wore the planned outfit or swapped it
   */
  static async confirmWear(userId: string, id: string, input: ConfirmWearLogInput): Promise<WearLog> {
    const existing = await this.getById(userId, id);
    if (!existing) throw new Error('Wear log not found');

    const now = new Date().toISOString();
    let finalOutfitId = existing.outfitId;
    let finalItemIds = existing.itemIds;

    if (input.replacementOutfitId && input.replacementOutfitId !== existing.outfitId) {
      const replacementOutfit = await OutfitService.getById(userId, input.replacementOutfitId);
      if (replacementOutfit) {
        finalOutfitId = replacementOutfit.id;
        finalItemIds = replacementOutfit.itemIds;
      }
    }

    db.update(wearLogs)
      .set({
        outfitId: finalOutfitId,
        itemIds: JSON.stringify(finalItemIds),
        status: input.status,
        notes: input.notes ?? existing.notes,
        confirmedAt: now,
        updatedAt: now
      })
      .where(and(eq(wearLogs.id, id), eq(wearLogs.userId, userId)))
      .run();

    if (input.status === 'worn') {
      await this.applyWears(userId, finalOutfitId ?? undefined, finalItemIds);
    }

    return (await this.getById(userId, id))!;
  }

  /**
   * Internal: Increment wear counts, flag laundry if max wears exceeded, update lastWornAt
   */
  private static async applyWears(userId: string, outfitId?: string, itemIds: string[] = []) {
    const now = new Date().toISOString();

    // 1. Update outfit wear count and lastWornAt
    if (outfitId) {
      const outfitRow = db.select().from(outfits).where(eq(outfits.id, outfitId)).get();
      if (outfitRow) {
        db.update(outfits)
          .set({
            wearCount: outfitRow.wearCount + 1,
            lastWornAt: now,
            updatedAt: now
          })
          .where(eq(outfits.id, outfitId))
          .run();
      }
    }

    // 2. Increment wears on each clothing item and flag needs_wash if threshold reached
    for (const itemId of itemIds) {
      const item = await ClothingItemService.getById(userId, itemId);
      if (!item) continue;

      const newWears = item.wearsSinceWash + 1;
      const willNeedWash = newWears >= item.maxWearsBeforeWash;

      db.update(clothingItems)
        .set({
          wearsSinceWash: newWears,
          totalLifetimeWears: item.totalLifetimeWears + 1,
          status: willNeedWash ? 'needs_wash' : item.status,
          lastWornAt: now,
          updatedAt: now
        })
        .where(eq(clothingItems.id, itemId))
        .run();
    }
  }
}
