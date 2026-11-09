import { FastifyInstance, FastifyReply } from 'fastify';
import { BackupService } from '../services/backup.js';

export async function backupRoutes(fastify: FastifyInstance) {
  fastify.addHook('preHandler', fastify.authenticate);

  // 1-Click Export ZIP archive
  fastify.get('/export', async (request: any, reply: FastifyReply) => {
    const zipBuffer = await BackupService.exportUserData(request.user.id);
    const dateStr = new Date().toISOString().split('T')[0];
    const filename = `trousseau-backup-${dateStr}.zip`;

    reply.header('Content-Type', 'application/zip');
    reply.header('Content-Disposition', `attachment; filename="${filename}"`);
    return reply.send(zipBuffer);
  });

  // 1-Click Import ZIP archive
  fastify.post('/import', async (request: any, reply: FastifyReply) => {
    const data = await request.file();
    if (!data) return reply.status(400).send({ error: 'No backup ZIP file provided' });

    const buffer = await data.toBuffer();
    const result = await BackupService.importUserData(request.user.id, buffer);
    return reply.send({ success: true, ...result });
  });
}
