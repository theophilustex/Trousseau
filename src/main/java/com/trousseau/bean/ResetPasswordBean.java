package com.trousseau.bean;

import com.trousseau.model.User;
import com.trousseau.service.UserService;

import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.view.ViewScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;

@Named
@ViewScoped
public class ResetPasswordBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private UserService userService;

    private String token;
    private String newPassword;
    private String confirmPassword;

    private boolean tokenValid;
    private boolean completed;
    private String maskedUsername;

    @PostConstruct
    public void init() {
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx == null) {
            return;
        }
        String t = ctx.getExternalContext().getRequestParameterMap().get("token");
        if (t != null && !t.isEmpty()) {
            this.token = t;
            User user = userService.findByValidResetToken(t);
            if (user != null) {
                tokenValid = true;
                maskedUsername = user.getUsername();
            }
        }
    }

    public String submit() {
        if (!tokenValid) {
            return null;
        }
        if (newPassword == null || newPassword.length() < 6) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Password must be at least 6 characters", null));
            return null;
        }
        if (!newPassword.equals(confirmPassword)) {
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Passwords do not match", null));
            return null;
        }
        boolean ok = userService.completePasswordReset(token, newPassword);
        if (!ok) {
            tokenValid = false;
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR,
                            "Reset link is invalid or has expired. Please request a new one.", null));
            return null;
        }
        completed = true;
        newPassword = null;
        confirmPassword = null;
        return null;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public boolean isTokenValid() {
        return tokenValid;
    }

    public boolean isCompleted() {
        return completed;
    }

    public String getMaskedUsername() {
        return maskedUsername;
    }
}
