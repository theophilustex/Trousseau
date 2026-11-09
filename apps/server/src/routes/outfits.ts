import { FastifyInstance, FastifyReply } from 'fastify';
import { CreateOutfitSchema, UpdateOutfitSchema } from '@trousseau/shared';
import { OutfitService } from '../services/outfits.js';

export async function outfitRoutes(fastify: FastifyInstance) {
  // Public community feed
  fastify.get('/public', async (_request: any, reply: FastifyReply) => {
    const outfits = await OutfitService.listPublic();
    return reply.send({ outfits });
  });

  // Authenticated routes
  fastify.register(async (authGroup) => {
    authGroup.addHook('preHandler', fastify.authenticate);

    authGroup.get('/', async (request: any, reply: FastifyReply) => {
      const { occasion, season } = request.query as any;
      const outfits = await OutfitService.list(request.user.id, { occasion, season });
      return reply.send({ outfits });
    });

    authGroup.get('/:id', async (request: any, reply: FastifyReply) => {
      const outfit = await OutfitService.getById(request.user.id, request.params.id);
      if (!outfit) return reply.status(404).send({ error: 'Outfit not found' });
      return reply.send({ outfit });
    });

    authGroup.post('/', async (request: any, reply: FastifyReply) => {
      const input = CreateOutfitSchema.parse(request.body);
      const outfit = await OutfitService.create(request.user.id, input);
      return reply.status(201).send({ outfit });
    });

    authGroup.patch('/:id', async (request: any, reply: FastifyReply) => {
      const input = UpdateOutfitSchema.parse(request.body);
      const outfit = await OutfitService.update(request.user.id, request.params.id, input);
      return reply.send({ outfit });
    });

    authGroup.delete('/:id', async (request: any, reply: FastifyReply) => {
      const success = await OutfitService.delete(request.user.id, request.params.id);
      if (!success) return reply.status(404).send({ error: 'Outfit not found' });
      return reply.send({ success: true });
    });
  });
}
