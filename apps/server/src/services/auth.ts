import bcrypt from 'bcryptjs';
import { nanoid } from 'nanoid';
import { eq, or } from 'drizzle-orm';
import { db } from '../db/index.js';
import { users } from '../db/schema.js';
import type { RegisterInput, LoginInput, User } from '@trousseau/shared';

export class AuthService {
  static async register(input: RegisterInput): Promise<User> {
    const existing = db.select()
      .from(users)
      .where(or(eq(users.username, input.username), eq(users.email, input.email)))
      .get();

    if (existing) {
      throw new Error('Username or email already exists');
    }

    const salt = bcrypt.genSaltSync(10);
    const passwordHash = bcrypt.hashSync(input.password, salt);
    const now = new Date().toISOString();
    const id = nanoid();

    const newUser = {
      id,
      username: input.username,
      passwordHash,
      displayName: input.displayName,
      email: input.email,
      locationCity: input.locationCity ?? null,
      locationLat: input.locationLat ?? null,
      locationLon: input.locationLon ?? null,
      createdAt: now,
      updatedAt: now
    };

    db.insert(users).values(newUser).run();

    return {
      id: newUser.id,
      username: newUser.username,
      displayName: newUser.displayName,
      email: newUser.email,
      locationCity: newUser.locationCity,
      locationLat: newUser.locationLat,
      locationLon: newUser.locationLon,
      createdAt: newUser.createdAt,
      updatedAt: newUser.updatedAt
    };
  }

  static async login(input: LoginInput): Promise<User> {
    const user = db.select()
      .from(users)
      .where(or(eq(users.username, input.usernameOrEmail), eq(users.email, input.usernameOrEmail)))
      .get();

    if (!user) {
      throw new Error('Invalid credentials');
    }

    const isValid = bcrypt.compareSync(input.password, user.passwordHash);
    if (!isValid) {
      throw new Error('Invalid credentials');
    }

    return {
      id: user.id,
      username: user.username,
      displayName: user.displayName,
      email: user.email,
      locationCity: user.locationCity,
      locationLat: user.locationLat,
      locationLon: user.locationLon,
      createdAt: user.createdAt,
      updatedAt: user.updatedAt
    };
  }

  static async getUserById(id: string): Promise<User | null> {
    const user = db.select().from(users).where(eq(users.id, id)).get();
    if (!user) return null;

    return {
      id: user.id,
      username: user.username,
      displayName: user.displayName,
      email: user.email,
      locationCity: user.locationCity,
      locationLat: user.locationLat,
      locationLon: user.locationLon,
      createdAt: user.createdAt,
      updatedAt: user.updatedAt
    };
  }

  static async updateLocation(id: string, city?: string, lat?: number, lon?: number): Promise<User> {
    const now = new Date().toISOString();
    db.update(users)
      .set({
        locationCity: city ?? null,
        locationLat: lat ?? null,
        locationLon: lon ?? null,
        updatedAt: now
      })
      .where(eq(users.id, id))
      .run();

    const updated = await this.getUserById(id);
    if (!updated) throw new Error('User not found');
    return updated;
  }
}
