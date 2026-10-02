package com.trousseau.filter;

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

@WebFilter(filterName = "AuthFilter", urlPatterns = {"*.xhtml"})
public class AuthFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        HttpSession session = req.getSession(false);

        String uri = req.getRequestURI();

        boolean isPublicPage = uri.contains("/login.xhtml")
                || uri.contains("/register.xhtml")
                || uri.contains("/index.xhtml");
        boolean isResourceRequest = uri.contains("/javax.faces.resource")
                || uri.contains("/resources/");
        boolean isLoggedIn = session != null
                && session.getAttribute("com.trousseau.loggedIn") != null;

        if (isPublicPage || isResourceRequest || isLoggedIn) {
            chain.doFilter(request, response);
        } else {
            res.sendRedirect(req.getContextPath() + "/login.xhtml");
        }
    }

    @Override
    public void destroy() {
    }
}
