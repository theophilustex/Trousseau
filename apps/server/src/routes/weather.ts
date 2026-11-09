import { FastifyInstance, FastifyReply } from 'fastify';
import { WeatherService } from '../services/weather.js';
import { AuthService } from '../services/auth.js';

export async function weatherRoutes(fastify: FastifyInstance) {
  fastify.addHook('preHandler', fastify.authenticate);

  fastify.get('/current', async (request: any, reply: FastifyReply) => {
    const user = await AuthService.getUserById(request.user.id);
    const lat = request.query.lat ? Number(request.query.lat) : (user?.locationLat ?? 40.7128);
    const lon = request.query.lon ? Number(request.query.lon) : (user?.locationLon ?? -74.0060);
    const city = request.query.city || user?.locationCity || 'Local';

    const forecast = await WeatherService.getForecast(lat, lon, city);
    return reply.send({ forecast });
  });
}
