import Fastify from 'fastify';
import cors from '@fastify/cors';
import jwt from '@fastify/jwt';
import multipart from '@fastify/multipart';
import fastifyStatic from '@fastify/static';
import dotenv from 'dotenv';
import path from 'node:path';
import fs from 'node:fs';
import { ZodError } from 'zod';

import { storagePaths } from './db/index.js';
import { runMigrations } from './db/migrate.js';
import { authRoutes } from './routes/auth.js';
import { itemRoutes } from './routes/items.js';
import { outfitRoutes } from './routes/outfits.js';
import { wearLogRoutes } from './routes/wear-logs.js';
import { socialRoutes } from './routes/social.js';
import { weatherRoutes } from './routes/weather.js';
import { recommendationRoutes } from './routes/recommendations.js';
import { backupRoutes } from './routes/backup.js';

dotenv.config();

// Ensure database schema is migrated
runMigrations();

const server = Fastify({
  logger: process.env.NODE_ENV !== 'test',
  bodyLimit: 50 * 1024 * 1024 // 50MB for backups and photo uploads
});

// Global Friendly Error Handler
server.setErrorHandler((error: any, request, reply) => {
  if (error instanceof ZodError) {
    const firstIssue = error.issues[0];
    const field = firstIssue?.path.join('.') || 'input';
    const message = firstIssue?.message || 'Invalid input';
    return reply.status(400).send({
      error: `${field}: ${message}`,
      details: error.issues
    });
  }

  if (error.statusCode && error.statusCode < 500) {
    return reply.status(error.statusCode).send({ error: error.message });
  }

  // Handle common business logic errors (e.g. unique constraint, invalid login)
  if (error.message && (
    error.message.includes('already exists') ||
    error.message.includes('Invalid credentials') ||
    error.message.includes('not found')
  )) {
    return reply.status(400).send({ error: error.message });
  }

  server.log.error(error);
  return reply.status(error.statusCode || 500).send({
    error: error.message || 'An unexpected error occurred'
  });
});

// CORS support for web and mobile clients
await server.register(cors, {
  origin: true,
  credentials: true
});

// Handle empty JSON bodies gracefully for DELETE and bodyless POST requests (e.g. /wash)
server.addContentTypeParser('application/json', { parseAs: 'string' }, (req, body, done) => {
  if (!body || (typeof body === 'string' && body.trim() === '')) {
    done(null, undefined);
    return;
  }
  try {
    done(null, JSON.parse(body));
  } catch (err: any) {
    err.statusCode = 400;
    done(err, undefined);
  }
});

// JWT Authentication
const JWT_SECRET = process.env.JWT_SECRET || 'trousseau_dev_secret_key_change_in_production';
await server.register(jwt, {
  secret: JWT_SECRET
});

// Decorate fastify with auth guard
server.decorate('authenticate', async (request: any, reply: any) => {
  try {
    await request.jwtVerify();
  } catch (err) {
    reply.status(401).send({ error: 'Unauthorized: authentication token required' });
  }
});

// Multipart support for file uploads and zip import
await server.register(multipart, {
  limits: {
    fileSize: 50 * 1024 * 1024 // 50MB
  }
});

// Static file serving for photos and thumbnails
await server.register(fastifyStatic, {
  root: storagePaths.uploadsDir,
  prefix: '/uploads/'
});

// Register API Routes
await server.register(authRoutes, { prefix: '/api/auth' });
await server.register(itemRoutes, { prefix: '/api/items' });
await server.register(outfitRoutes, { prefix: '/api/outfits' });
await server.register(wearLogRoutes, { prefix: '/api/wear-logs' });
await server.register(socialRoutes, { prefix: '/api/social' });
await server.register(weatherRoutes, { prefix: '/api/weather' });
await server.register(recommendationRoutes, { prefix: '/api/recommendations' });
await server.register(backupRoutes, { prefix: '/api/backup' });

// Health Check
server.get('/api/health', async () => {
  return {
    status: 'ok',
    app: 'Trousseau',
    version: '0.1.0',
    timestamp: new Date().toISOString()
  };
});

// Serve compiled web frontend in production if available
const webDistPath = path.resolve(process.cwd(), '../web/dist');
const altWebDistPath = path.resolve(process.cwd(), 'web-dist');
const resolvedWebPath = fs.existsSync(altWebDistPath) ? altWebDistPath : fs.existsSync(webDistPath) ? webDistPath : null;

if (resolvedWebPath) {
  await server.register(fastifyStatic, {
    root: resolvedWebPath,
    prefix: '/',
    decorateReply: false
  });

  server.setNotFoundHandler((request, reply) => {
    if (request.raw.url && (request.raw.url.startsWith('/api') || request.raw.url.startsWith('/uploads'))) {
      reply.status(404).send({ error: 'Endpoint not found' });
    } else {
      reply.sendFile('index.html', resolvedWebPath);
    }
  });
}

// Start server
const PORT = process.env.PORT ? parseInt(process.env.PORT, 10) : 4000;
const HOST = process.env.HOST || '0.0.0.0';

try {
  await server.listen({ port: PORT, host: HOST });
  console.log(`Trousseau server is running at http://${HOST}:${PORT}`);
} catch (err) {
  server.log.error(err);
  process.exit(1);
}

export default server;
