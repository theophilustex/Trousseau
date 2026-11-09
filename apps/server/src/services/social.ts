import { eq, and, desc } from 'drizzle-orm';
import { nanoid } from 'nanoid';
import { db } from '../db/index.js';
import { ratings, comments } from '../db/schema.js';
import {
  type Rating,
  type CreateRatingInput,
  type Comment,
  type CreateCommentInput
} from '@trousseau/shared';

export class SocialService {
  static formatRating(row: typeof ratings.$inferSelect): Rating {
    return {
      id: row.id,
      outfitId: row.outfitId,
      userId: row.userId,
      raterType: row.raterType as any,
      raterName: row.raterName,
      stars: row.stars,
      feedback: row.feedback,
      createdAt: row.createdAt,
      updatedAt: row.updatedAt
    };
  }

  static formatComment(row: typeof comments.$inferSelect): Comment {
    return {
      id: row.id,
      outfitId: row.outfitId,
      userId: row.userId,
      userName: row.userName,
      content: row.content,
      createdAt: row.createdAt,
      updatedAt: row.updatedAt
    };
  }

  // --- Ratings ---

  static async listRatings(outfitId: string): Promise<Rating[]> {
    const rows = db.select()
      .from(ratings)
      .where(eq(ratings.outfitId, outfitId))
      .orderBy(desc(ratings.createdAt))
      .all();

    return rows.map(this.formatRating);
  }

  static async addRating(userId: string, outfitId: string, input: CreateRatingInput): Promise<Rating> {
    const id = nanoid();
    const now = new Date().toISOString();

    const newRow = {
      id,
      outfitId,
      userId,
      raterType: input.raterType,
      raterName: input.raterName,
      stars: input.stars,
      feedback: input.feedback ?? null,
      createdAt: now,
      updatedAt: now
    };

    db.insert(ratings).values(newRow).run();

    const created = db.select().from(ratings).where(eq(ratings.id, id)).get();
    if (!created) throw new Error('Failed to create rating');
    return this.formatRating(created);
  }

  static async deleteRating(userId: string, ratingId: string): Promise<boolean> {
    const res = db.delete(ratings)
      .where(and(eq(ratings.id, ratingId), eq(ratings.userId, userId)))
      .run();

    return res.changes > 0;
  }

  // --- Comments ---

  static async listComments(outfitId: string): Promise<Comment[]> {
    const rows = db.select()
      .from(comments)
      .where(eq(comments.outfitId, outfitId))
      .orderBy(desc(comments.createdAt))
      .all();

    return rows.map(this.formatComment);
  }

  static async addComment(userId: string, userName: string, outfitId: string, input: CreateCommentInput): Promise<Comment> {
    const id = nanoid();
    const now = new Date().toISOString();

    const newRow = {
      id,
      outfitId,
      userId,
      userName,
      content: input.content,
      createdAt: now,
      updatedAt: now
    };

    db.insert(comments).values(newRow).run();

    const created = db.select().from(comments).where(eq(comments.id, id)).get();
    if (!created) throw new Error('Failed to post comment');
    return this.formatComment(created);
  }

  static async deleteComment(userId: string, commentId: string): Promise<boolean> {
    const res = db.delete(comments)
      .where(and(eq(comments.id, commentId), eq(comments.userId, userId)))
      .run();

    return res.changes > 0;
  }
}
