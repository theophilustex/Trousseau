package com.trousseau.service;

import com.trousseau.model.User;
import com.trousseau.util.AppConfig;
import com.trousseau.util.HtmlUtil;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * "Forgot password": emails a single-use link to the account's address.
 *
 * <p>The link only ever goes to the email on file, never to the person filling in the
 * form, and {@link #requestReset} reveals nothing about whether an account matched.
 * An earlier version displayed the link on screen, which let anyone who knew a
 * username take over that account.</p>
 */
@Stateless
public class PasswordResetService {

    private static final Logger LOG = Logger.getLogger(PasswordResetService.class.getName());

    /** At most one email per account per this many minutes, so the form can't flood an inbox. */
    static final int RESEND_INTERVAL_MINUTES = 2;

    @Inject
    private UserService userService;

    @Inject
    private MailService mailService;

    /**
     * Whether reset emails can be sent at all. Without a configured base URL there is no
     * safe way to build the link, so the page says the feature is unavailable instead.
     */
    public boolean isAvailable() {
        return AppConfig.baseUrl().isPresent();
    }

    /**
     * Emails a reset link if the input names an account that has an email address.
     *
     * <p>Returns nothing on purpose: the caller must respond identically whether or not
     * an account matched, so the form can't be used to discover accounts. The email is
     * sent in the background for the same reason, since the SMTP round trip would
     * otherwise make matching requests visibly slower.</p>
     */
    public void requestReset(String usernameOrEmail) {
        Optional<String> baseUrl = AppConfig.baseUrl();
        if (baseUrl.isEmpty()) {
            LOG.warning("Password reset requested, but TROUSSEAU_BASE_URL is not set; no email sent");
            return;
        }

        User user = userService.findByUsernameOrEmail(usernameOrEmail);
        if (user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            return;
        }
        if (userService.isResetRequestedWithin(user, RESEND_INTERVAL_MINUTES)) {
            return;
        }

        String token = userService.issuePasswordResetToken(user);
        String link = baseUrl.get() + "/reset-password.xhtml?token=" + token;
        mailService.sendInBackground(user.getEmail(), "Reset your Trousseau password", buildBody(user, link));
    }

    private String buildBody(User user, String link) {
        String name = HtmlUtil.escape(user.getDisplayName() != null ? user.getDisplayName() : user.getUsername());
        String href = HtmlUtil.escape(link);
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'></head>"
                + "<body style='font-family:Helvetica,Arial,sans-serif;color:#1e293b;max-width:560px;margin:0 auto;padding:24px'>"
                + "<h2 style='color:#6366f1'>Reset your password</h2>"
                + "<p>Hi " + name + ",</p>"
                + "<p>Someone asked to reset the password for your Trousseau account <strong>"
                + HtmlUtil.escape(user.getUsername()) + "</strong>. If that was you, use this link "
                + "within " + UserService.RESET_TOKEN_TTL_MINUTES + " minutes:</p>"
                + "<p><a href='" + href + "' style='display:inline-block;background:#6366f1;color:#fff;"
                + "padding:10px 18px;border-radius:8px;text-decoration:none'>Set a new password</a></p>"
                + "<p style='font-size:13px;color:#64748b'>Or paste this into your browser:<br>" + href + "</p>"
                + "<p style='font-size:13px;color:#64748b'>If you didn't ask for this, ignore this email. "
                + "Your password stays the same.</p>"
                + "</body></html>";
    }
}
