import { FastifyInstance, FastifyReply } from 'fastify';
import { CreateWearLogSchema, ConfirmWearLogSchema } from '@trousseau/shared';
import { WearLogService } from '../services/wear-logs.js';

export async function wearLogRoutes(fastify: FastifyInstance) {
  fastify.addHook('preHandler', fastify.authenticate);

  // History list
  fastify.get('/', async (request: any, reply: FastifyReply) => {
    const limit = request.query.limit ? Number(request.query.limit) : 50;
    const logs = await WearLogService.list(request.user.id, limit);
    return reply.send({ logs });
  });

  // Today's log / plan
  fastify.get('/today', async (request: any, reply: FastifyReply) => {
    const today = new Date().toISOString().split('T')[0];
    const log = await WearLogService.getByDate(request.user.id, today);
    return reply.send({ log, date: today });
  });

  // Create or plan a wear log
  fastify.post('/', async (request: any, reply: FastifyReply) => {
    const input = CreateWearLogSchema.parse(request.body);
    const log = await WearLogService.create(request.user.id, input);
    return reply.status(201).send({ log });
  });

  // Mobile check-in confirmation
  fastify.post('/:id/confirm', async (request: any, reply: FastifyReply) => {
    const input = ConfirmWearLogSchema.parse(request.body);
    const log = await WearLogService.confirmWear(request.user.id, request.params.id, input);
    return reply.send({ log });
  });

  // Update notes or details
  fastify.patch('/:id', async (request: any, reply: FastifyReply) => {
    const log = await WearLogService.update(request.user.id, request.params.id, request.body as any);
    return reply.send({ log });
  });
}
