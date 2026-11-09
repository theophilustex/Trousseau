import { FastifyInstance, FastifyReply } from 'fastify';
import { RecommendationService } from '../services/recommendations.js';

export async function recommendationRoutes(fastify: FastifyInstance) {
  fastify.addHook('preHandler', fastify.authenticate);

  fastify.get('/', async (request: any, reply: FastifyReply) => {
    const { occasion, targetDate } = request.query as any;
    const recommendations = await RecommendationService.getRecommendations(request.user.id, {
      occasion,
      targetDate
    });
    return reply.send({ recommendations });
  });
}
