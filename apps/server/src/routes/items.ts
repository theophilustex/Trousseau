import { FastifyInstance, FastifyReply } from 'fastify';
import { CreateClothingItemSchema, UpdateClothingItemSchema } from '@trousseau/shared';
import { ClothingItemService } from '../services/items.js';
import { MediaService } from '../services/media.js';

export async function itemRoutes(fastify: FastifyInstance) {
  // All clothing routes require authentication
  fastify.addHook('preHandler', fastify.authenticate);

  // List items with filters
  fastify.get('/', async (request: any, reply: FastifyReply) => {
    const filters = request.query as any;
    const items = await ClothingItemService.list(request.user.id, {
      category: filters.category,
      status: filters.status,
      season: filters.season,
      tag: filters.tag,
      search: filters.search,
      cleanOnly: filters.cleanOnly === 'true'
    });
    return reply.send({ items });
  });

  // Get single item
  fastify.get('/:id', async (request: any, reply: FastifyReply) => {
    const item = await ClothingItemService.getById(request.user.id, request.params.id);
    if (!item) return reply.status(404).send({ error: 'Item not found' });
    return reply.send({ item });
  });

  // Create item
  fastify.post('/', async (request: any, reply: FastifyReply) => {
    const input = CreateClothingItemSchema.parse(request.body);
    const item = await ClothingItemService.create(request.user.id, input);
    return reply.status(201).send({ item });
  });

  // Update item
  fastify.patch('/:id', async (request: any, reply: FastifyReply) => {
    const input = UpdateClothingItemSchema.parse(request.body);
    const item = await ClothingItemService.update(request.user.id, request.params.id, input);
    return reply.send({ item });
  });

  // Delete item
  fastify.delete('/:id', async (request: any, reply: FastifyReply) => {
    const success = await ClothingItemService.delete(request.user.id, request.params.id);
    if (!success) return reply.status(404).send({ error: 'Item not found' });
    return reply.send({ success: true });
  });

  // Wash single item
  fastify.post('/:id/wash', async (request: any, reply: FastifyReply) => {
    const item = await ClothingItemService.washItem(request.user.id, request.params.id);
    return reply.send({ item });
  });

  // Wash all dirty items
  fastify.post('/wash-all', async (request: any, reply: FastifyReply) => {
    const count = await ClothingItemService.washAllDirty(request.user.id);
    return reply.send({ washedCount: count });
  });

  // Upload image with optional local background removal
  fastify.post('/upload', async (request: any, reply: FastifyReply) => {
    const data = await request.file();
    if (!data) return reply.status(400).send({ error: 'No image uploaded' });

    const buffer = await data.toBuffer();
    const removeBg = data.fields?.removeBackground?.value === 'true' || request.query.removeBackground === 'true';

    const processed = await MediaService.processImage(buffer, {
      removeBackground: removeBg
    });

    return reply.send(processed);
  });
}
