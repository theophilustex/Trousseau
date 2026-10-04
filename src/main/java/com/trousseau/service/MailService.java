package com.trousseau.service;

import jakarta.annotation.Resource;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.Stateless;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sends HTML email through the server's default mail session.
 *
 * <p>Configure the session on WildFly (see docs/setup-and-deployment.md); in the
 * Docker image it is wired to the SMTP_* environment variables.</p>
 */
@Stateless
public class MailService {

    private static final Logger LOG = Logger.getLogger(MailService.class.getName());

    /** Used when the mail session has no "from" attribute. */
    static final String DEFAULT_FROM = "noreply@trousseau.app";

    @Resource(lookup = "java:jboss/mail/Default")
    private Session mailSession;

    /** Sends now, throwing on failure so the caller can decide what that means. */
    public void send(String to, String subject, String html) throws MessagingException {
        if (mailSession == null) {
            throw new MessagingException("No mail session is configured");
        }
        // The sender comes from the mail session's "from" attribute (mail.from), so
        // each deployment can use an address its SMTP provider accepts.
        String from = mailSession.getProperty("mail.from");

        MimeMessage msg = new MimeMessage(mailSession);
        msg.setFrom(new InternetAddress(from != null && !from.isBlank() ? from : DEFAULT_FROM, false));
        msg.setRecipient(Message.RecipientType.TO, new InternetAddress(to));
        msg.setSubject(subject, "UTF-8");
        msg.setContent(html, "text/html; charset=UTF-8");
        Transport.send(msg);
    }

    /**
     * Sends on a container thread and returns immediately; failures are logged. Use it
     * when the caller must not wait on SMTP, or must not take visibly longer depending
     * on whether an email was sent at all.
     */
    @Asynchronous
    public void sendInBackground(String to, String subject, String html) {
        try {
            send(to, subject, html);
        } catch (MessagingException | RuntimeException e) {
            LOG.log(Level.WARNING, "Failed to send \"" + subject + "\" to " + to, e);
        }
    }
}
