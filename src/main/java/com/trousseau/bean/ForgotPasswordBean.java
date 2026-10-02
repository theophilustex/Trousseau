package com.trousseau.bean;

import com.trousseau.service.UserService;

import javax.enterprise.context.RequestScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

@Named
@RequestScoped
public class ForgotPasswordBean {

    @Inject
    private UserService userService;

    private String usernameOrEmail;

    private boolean submitted;
    private String resetUrl;

    public String submit() {
        String token = userService.requestPasswordReset(usernameOrEmail);
        submitted = true;
        if (token != null) {
            resetUrl = buildResetUrl(token);
        }
        return null;
    }

    private String buildResetUrl(String token) {
        FacesContext ctx = FacesContext.getCurrentInstance();
        ExternalContext ec = ctx.getExternalContext();
        HttpServletRequest req = (HttpServletRequest) ec.getRequest();

        StringBuilder base = new StringBuilder();
        base.append(req.getScheme()).append("://").append(req.getServerName());
        int port = req.getServerPort();
        boolean defaultPort = ("http".equals(req.getScheme()) && port == 80)
                || ("https".equals(req.getScheme()) && port == 443);
        if (!defaultPort) {
            base.append(':').append(port);
        }
        base.append(req.getContextPath());
        base.append("/reset-password.xhtml?token=").append(token);
        return base.toString();
    }

    public String getUsernameOrEmail() {
        return usernameOrEmail;
    }

    public void setUsernameOrEmail(String usernameOrEmail) {
        this.usernameOrEmail = usernameOrEmail;
    }

    public boolean isSubmitted() {
        return submitted;
    }

    public String getResetUrl() {
        return resetUrl;
    }

    public boolean isResetUrlAvailable() {
        return resetUrl != null;
    }
}
