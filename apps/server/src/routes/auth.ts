import { FastifyInstance, FastifyRequest, FastifyReply } from 'fastify';
import { RegisterInputSchema, LoginInputSchema } from '@trousseau/shared';
import { AuthService } from '../services/auth.js';

export async function authRoutes(fastify: FastifyInstance) {
  fastify.post('/register', async (request: FastifyRequest, reply: FastifyReply) => {
    const input = RegisterInputSchema.parse(request.body);
    const user = await AuthService.register(input);
    const token = fastify.jwt.sign({ id: user.id, username: user.username });
    return reply.status(201).send({ token, user });
  });

  fastify.post('/login', async (request: FastifyRequest, reply: FastifyReply) => {
    const input = LoginInputSchema.parse(request.body);
    const user = await AuthService.login(input);
    const token = fastify.jwt.sign({ id: user.id, username: user.username });
    return reply.send({ token, user });
  });

  fastify.get('/me', {
    preHandler: [fastify.authenticate]
  }, async (request: any, reply: FastifyReply) => {
    const user = await AuthService.getUserById(request.user.id);
    if (!user) return reply.status(404).send({ error: 'User not found' });
    return reply.send({ user });
  });

  fastify.patch('/location', {
    preHandler: [fastify.authenticate]
  }, async (request: any, reply: FastifyReply) => {
    const { city, lat, lon } = request.body as any;
    const user = await AuthService.updateLocation(request.user.id, city, lat, lon);
    return reply.send({ user });
  });
}
