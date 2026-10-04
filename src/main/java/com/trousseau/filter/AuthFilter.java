package com.trousseau.filter;

import com.trousseau.bean.SessionBean;
import com.trousseau.service.UserService;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Sends anyone not signed in to the login page, except for the public pages below.
 *
 * <p>Decisions are made on {@link HttpServletRequest#getServletPath()}, which the
 * container has already normalised, and never on the raw request URI. The raw URI is
 * attacker-shaped: an earlier version matched {@code uri.contains("/login.xhtml")},
 * and {@code /login.xhtml/../wardrobe.xhtml} passed that check and was then served as
 * the wardrobe page.</p>
 *
 * <p>It also ends sessions whose credentials are out of date: a session records the
 * account's credentials version at login, and once a password change or reset has moved
 * the version on, the session is invalidated on its next request. That is what signs out
 * other devices after a password change.</p>
 */
@WebFilter(filterName = "AuthFilter", urlPatterns = {"*.xhtml"})
public class AuthFilter implements Filter {

    private static final Set<String> PUBLIC_PAGES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "/index.xhtml",
            "/login.xhtml",
            "/register.xhtml",
            "/forgot-password.xhtml",
            "/reset-password.xhtml")));

    /** JSF resources (CSS, JS, images, PrimeFaces dynamic content). */
    private static final String JSF_RESOURCE_PREFIX = "/javax.faces.resource/";

    /** PrimeFaces' streamed-content endpoint, which serves users' photos. */
    private static final String DYNAMIC_CONTENT_PREFIX = JSF_RESOURCE_PREFIX + "dynamiccontent";

    @Inject
    private UserService userService;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        HttpSession session = req.getSession(false);

        String path = req.getServletPath();

        boolean isPublic = PUBLIC_PAGES.contains(path) || path.startsWith(JSF_RESOURCE_PREFIX);
        boolean isLoggedIn = session != null
                && session.getAttribute(SessionBean.LOGGED_IN_ATTR) != null;

        // Static CSS and JS skip the database check; pages and photos do not.
        boolean isStaticResource = path.startsWith(JSF_RESOURCE_PREFIX)
                && !path.startsWith(DYNAMIC_CONTENT_PREFIX);
        if (isLoggedIn && !isStaticResource && !credentialsCurrent(session)) {
            session.invalidate();
            isLoggedIn = false;
        }

        if (isPublic || isLoggedIn) {
            chain.doFilter(request, response);
        } else {
            res.sendRedirect(req.getContextPath() + "/login.xhtml");
        }
    }

    /** True while the account's password hasn't changed since this session recorded it. */
    private boolean credentialsCurrent(HttpSession session) {
        Object userId = session.getAttribute(SessionBean.USER_ID_ATTR);
        Object version = session.getAttribute(SessionBean.CREDENTIALS_VERSION_ATTR);
        if (!(userId instanceof Long) || !(version instanceof Integer)) {
            return false;
        }
        return userService.getCredentialsVersion((Long) userId) == (Integer) version;
    }

    @Override
    public void destroy() {
    }
}
