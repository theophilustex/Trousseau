package com.trousseau.bean;

import com.trousseau.model.ClothingItem;
import com.trousseau.service.ClothingItemService;
import com.trousseau.util.ImageUtil;

import javax.enterprise.context.ApplicationScoped;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.ByteArrayInputStream;

import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

@Named
@ApplicationScoped
public class ImageStreamer {

    @Inject
    private ClothingItemService clothingItemService;

    /** Full-resolution photo. Use this only where the image is displayed large. */
    public StreamedContent getImage() {
        Long itemId = requestedItemId();
        if (itemId == null) {
            return new DefaultStreamedContent();
        }

        ClothingItem item = clothingItemService.findById(itemId);
        if (item == null || item.getImageData() == null) {
            return new DefaultStreamedContent();
        }
        return DefaultStreamedContent.builder()
                .stream(() -> new ByteArrayInputStream(item.getImageData()))
                .contentType(item.getImageContentType())
                .name(item.getImageName())
                .build();
    }

    /**
     * Scaled-down copy for grids, cards, and planner tiles.
     *
     * <p>This is what every list view should bind to. The service generates the
     * thumbnail on first request for items that predate the feature, and falls back to
     * the original when the photo is already small enough to not be worth scaling.</p>
     */
    public StreamedContent getThumbnail() {
        Long itemId = requestedItemId();
        if (itemId == null) {
            return new DefaultStreamedContent();
        }

        byte[] data = clothingItemService.getThumbnailData(itemId);
        if (data == null || data.length == 0) {
            return new DefaultStreamedContent();
        }
        return DefaultStreamedContent.builder()
                .stream(() -> new ByteArrayInputStream(data))
                .contentType(ImageUtil.THUMBNAIL_CONTENT_TYPE)
                .build();
    }

    /**
     * The itemId request parameter, or null when there is none, when it is malformed,
     * or when this is the render pass.
     *
     * <p>PrimeFaces renders a streamed image in two passes: on RENDER_RESPONSE it only
     * emits the URL, and a second request actually fetches the bytes. Returning empty
     * content during the render pass is what makes that work.</p>
     */
    private Long requestedItemId() {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context == null || context.getCurrentPhaseId() == javax.faces.event.PhaseId.RENDER_RESPONSE) {
            return null;
        }
        String param = context.getExternalContext().getRequestParameterMap().get("itemId");
        if (param == null || param.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(param.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
