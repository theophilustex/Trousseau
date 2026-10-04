package com.trousseau.util;

/** Helpers for HTML built by hand, such as email bodies. */
public final class HtmlUtil {

    private HtmlUtil() {
    }

    /** Escapes text for inclusion in HTML element content or a quoted attribute. */
    public static String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
