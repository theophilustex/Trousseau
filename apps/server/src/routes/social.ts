import { FastifyInstance, FastifyReply } from 'fastify';
import { CreateRatingSchema, CreateCommentSchema } from '@trousseau/shared';
import { SocialService } from '../services/social.js';

export async function socialRoutes(fastify: FastifyInstance) {
  fastify.addHook('preHandler', fastify.authenticate);

  // Ratings
  fastify.get('/outfits/:id/ratings', async (request: any, reply: FastifyReply) => {
    const ratings = await SocialService.listRatings(request.params.id);
    return reply.send({ ratings });
  });

  fastify.post('/outfits/:id/ratings', async (request: any, reply: FastifyReply) => {
    const input = CreateRatingSchema.parse(request.body);
    const rating = await SocialService.addRating(request.user.id, request.params.id, input);
    return reply.status(201).send({ rating });
  });

  fastify.delete('/ratings/:id', async (request: any, reply: FastifyReply) => {
    const success = await SocialService.deleteRating(request.user.id, request.params.id);
    if (!success) return reply.status(404).send({ error: 'Rating not found' });
    return reply.send({ success: true });
  });

  // Comments
  fastify.get('/outfits/:id/comments', async (request: any, reply: FastifyReply) => {
    const comments = await SocialService.listComments(request.params.id);
    return reply.send({ comments });
  });

  fastify.post('/outfits/:id/comments', async (request: any, reply: FastifyReply) => {
    const input = CreateCommentSchema.parse(request.body);
    const comment = await SocialService.addComment(
      request.user.id,
      request.user.username || 'User',
      request.params.id,
      input
    );
    return reply.status(201).send({ comment });
  });

  fastify.delete('/comments/:id', async (request: any, reply: FastifyReply) => {
    const success = await SocialService.deleteComment(request.user.id, request.params.id);
    if (!success) return reply.status(404).send({ error: 'Comment not found' });
    return reply.send({ success: true });
  });
}
