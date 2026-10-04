package com.trousseau.bean;

import com.trousseau.service.PasswordResetService;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

/**
 * Backs forgot-password.xhtml. The response after submitting is the same whether or not
 * an account matched; the link itself only ever goes out by email.
 */
@Named
@RequestScoped
public class ForgotPasswordBean {

    @Inject
    private PasswordResetService passwordResetService;

    private String usernameOrEmail;

    private boolean submitted;

    public String submit() {
        passwordResetService.requestReset(usernameOrEmail);
        submitted = true;
        return null;
    }

    /** False when the server has no base URL configured, so no reset email can be sent. */
    public boolean isAvailable() {
        return passwordResetService.isAvailable();
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
}
