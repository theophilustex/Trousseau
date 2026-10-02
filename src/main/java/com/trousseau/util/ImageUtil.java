package com.trousseau.util;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Down-scales uploaded photos into grid-sized thumbnails.
 *
 * <p>Every wardrobe grid, outfit card, and planner tile draws small tiles. Serving the
 * original upload for those means a page with fifty items pulls fifty multi-megabyte
 * photos over the wire to render 200px squares. A stored thumbnail turns that into a
 * few hundred kilobytes.</p>
 */
public final class ImageUtil {

    private static final Logger LOG = Logger.getLogger(ImageUtil.class.getName());

    /** Longest edge of a generated thumbnail, in pixels. */
    public static final int THUMBNAIL_MAX_EDGE = 400;

    private static final String THUMBNAIL_FORMAT = "jpg";

    public static final String THUMBNAIL_CONTENT_TYPE = "image/jpeg";

    private ImageUtil() {
    }

    /**
     * Scales an image so its longest edge is at most {@link #THUMBNAIL_MAX_EDGE},
     * preserving aspect ratio, and encodes it as JPEG.
     *
     * <p>Returns {@code null} rather than throwing when the bytes are not a readable
     * image — a thumbnail is an optimisation, and failing to build one must never stop
     * an upload from being saved. Callers fall back to the original image.</p>
     */
    public static byte[] createThumbnail(byte[] original) {
        if (original == null || original.length == 0) {
            return null;
        }
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(original));
            if (source == null) {
                LOG.fine("Uploaded bytes were not a readable image; skipping thumbnail");
                return null;
            }

            int width = source.getWidth();
            int height = source.getHeight();
            if (width <= 0 || height <= 0) {
                return null;
            }

            // Already small enough: re-encoding would only lose quality for no gain.
            if (width <= THUMBNAIL_MAX_EDGE && height <= THUMBNAIL_MAX_EDGE) {
                return null;
            }

            double scale = (double) THUMBNAIL_MAX_EDGE / Math.max(width, height);
            int targetWidth = Math.max(1, (int) Math.round(width * scale));
            int targetHeight = Math.max(1, (int) Math.round(height * scale));

            // TYPE_INT_RGB, not ARGB: JPEG has no alpha channel, and writing an image
            // that has one produces colour-inverted output with some encoders.
            BufferedImage target = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = target.createGraphics();
            try {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                // Flatten any transparency onto white before drawing.
                g.setColor(java.awt.Color.WHITE);
                g.fillRect(0, 0, targetWidth, targetHeight);
                g.drawImage(source, 0, 0, targetWidth, targetHeight, null);
            } finally {
                g.dispose();
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            if (!ImageIO.write(target, THUMBNAIL_FORMAT, out)) {
                LOG.fine("No JPEG writer available; skipping thumbnail");
                return null;
            }
            return out.toByteArray();

        } catch (Exception e) {
            // Includes IOException and the OutOfMemoryError-adjacent failures ImageIO
            // can raise on hostile input. A missing thumbnail is recoverable; a failed
            // upload is not.
            LOG.log(Level.FINE, "Thumbnail generation failed; falling back to the original image", e);
            return null;
        }
    }
}
