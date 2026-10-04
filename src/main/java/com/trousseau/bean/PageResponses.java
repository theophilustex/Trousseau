package com.trousseau.bean;

import javax.faces.context.FacesContext;
import java.io.IOException;
import java.io.UncheckedIOException;

/** Responses a backing bean can end a page request with. */
final class PageResponses {

    private PageResponses() {
    }

    /**
     * Ends the request with 404. Detail pages use this both for ids that do not exist
     * and for ones the user may not see, so the response does not reveal which is which.
     */
    static void notFound() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        try {
            ctx.getExternalContext().responseSendError(404, "Not found");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        ctx.responseComplete();
    }
}
