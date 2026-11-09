import sharp from 'sharp';
import { nanoid } from 'nanoid';
import path from 'node:path';
import fs from 'node:fs';
import { storagePaths } from '../db/index.js';

export interface ProcessedMedia {
  imageUrl: string;
  thumbnailUrl: string;
  hasTransparentBg: boolean;
}

export class MediaService {
  /**
   * Process uploaded clothing photo into high-efficiency WebP + thumbnail,
   * with optional local background removal.
   */
  static async processImage(
    buffer: Buffer,
    options: { removeBackground?: boolean } = {}
  ): Promise<ProcessedMedia> {
    const fileId = nanoid();
    const imageFilename = `${fileId}.webp`;
    const thumbnailFilename = `${fileId}_thumb.webp`;

    const fullImagePath = path.join(storagePaths.imagesDir, imageFilename);
    const thumbnailPath = path.join(storagePaths.thumbnailsDir, thumbnailFilename);

    let processedPipeline = sharp(buffer).rotate(); // auto-orient based on EXIF

    let hasTransparentBg = false;

    if (options.removeBackground) {
      try {
        // High quality local edge and luminance-aware background masking
        // For clothing items photographed against light or uniform backgrounds
        const raw = await processedPipeline
          .ensureAlpha()
          .raw()
          .toBuffer({ resolveWithObject: true });

        const { data, info } = raw;
        const width = info.width;
        const height = info.height;
        const channels = info.channels;

        // Sample corner pixels to determine background reference color
        const cornerSamples = [
          [0, 0],
          [width - 1, 0],
          [0, height - 1],
          [width - 1, height - 1]
        ];

        let bgR = 0, bgG = 0, bgB = 0;
        for (const [cx, cy] of cornerSamples) {
          const idx = (cy * width + cx) * channels;
          bgR += data[idx];
          bgG += data[idx + 1];
          bgB += data[idx + 2];
        }
        bgR /= cornerSamples.length;
        bgG /= cornerSamples.length;
        bgB /= cornerSamples.length;

        // Apply distance tolerance to make background transparent
        const tolerance = 42;
        for (let i = 0; i < data.length; i += channels) {
          const r = data[i];
          const g = data[i + 1];
          const b = data[i + 2];

          const diff = Math.sqrt(
            Math.pow(r - bgR, 2) + Math.pow(g - bgG, 2) + Math.pow(b - bgB, 2)
          );

          if (diff < tolerance) {
            data[i + 3] = 0; // Transparent
          } else if (diff < tolerance + 15) {
            // Soft anti-aliased edge
            data[i + 3] = Math.round(((diff - tolerance) / 15) * 255);
          }
        }

        processedPipeline = sharp(data, {
          raw: {
            width,
            height,
            channels: 4
          }
        });

        hasTransparentBg = true;
      } catch (err) {
        console.warn('Local background removal fallback to standard processing:', err);
        processedPipeline = sharp(buffer).rotate();
      }
    }

    // Save main WebP image (max 1200px width/height, quality 85)
    await processedPipeline
      .resize({
        width: 1200,
        height: 1200,
        fit: 'inside',
        withoutEnlargement: true
      })
      .webp({ quality: 85, effort: 4 })
      .toFile(fullImagePath);

    // Save 320x320 square thumbnail (contain or cover with padding)
    await sharp(fullImagePath)
      .resize({
        width: 320,
        height: 320,
        fit: 'contain',
        background: { r: 245, g: 245, b: 247, alpha: hasTransparentBg ? 0 : 1 }
      })
      .webp({ quality: 80, effort: 3 })
      .toFile(thumbnailPath);

    return {
      imageUrl: `/uploads/images/${imageFilename}`,
      thumbnailUrl: `/uploads/thumbnails/${thumbnailFilename}`,
      hasTransparentBg
    };
  }

  /**
   * Delete image files from disk when a clothing item is deleted
   */
  static deleteFiles(imageUrl?: string | null, thumbnailUrl?: string | null) {
    if (imageUrl) {
      const filename = path.basename(imageUrl);
      const fullPath = path.join(storagePaths.imagesDir, filename);
      if (fs.existsSync(fullPath)) {
        try { fs.unlinkSync(fullPath); } catch {}
      }
    }
    if (thumbnailUrl) {
      const filename = path.basename(thumbnailUrl);
      const fullPath = path.join(storagePaths.thumbnailsDir, filename);
      if (fs.existsSync(fullPath)) {
        try { fs.unlinkSync(fullPath); } catch {}
      }
    }
  }
}
