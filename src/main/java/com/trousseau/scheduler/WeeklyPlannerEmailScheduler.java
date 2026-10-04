package com.trousseau.scheduler;

import com.trousseau.model.Outfit;
import com.trousseau.model.PlannedOutfit;
import com.trousseau.model.User;
import com.trousseau.service.UserService;
import com.trousseau.service.WeeklyPlannerService;

import javax.annotation.Resource;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Sends each user their weekly outfit plan every Sunday at 5 PM.
 *
 * Prerequisites — configure a mail session on WildFly:
 *   /subsystem=mail/mail-session=default:write-attribute(name=jndi-name,value=java:jboss/mail/Default)
 *   /subsystem=mail/mail-session=default/server=smtp:write-attribute(name=outbound-socket-binding-ref,value=mail-smtp)
 * Then set the outbound-socket-binding host/port in socket-binding-group to point to your SMTP server.
 */
@Singleton
@Startup
public class WeeklyPlannerEmailScheduler {

    private static final Logger LOG = Logger.getLogger(WeeklyPlannerEmailScheduler.class.getName());
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMMM d, yyyy");
    private static final String DEFAULT_FROM = "noreply@trousseau.app";

    @Inject
    private UserService userService;

    @Inject
    private WeeklyPlannerService weeklyPlannerService;

    // WildFly default mail session — configure in standalone.xml before use
    @Resource(lookup = "java:jboss/mail/Default")
    private Session mailSession;

    @Schedule(dayOfWeek = "Sun", hour = "17", minute = "0", second = "0", persistent = false)
    public void sendWeeklyPlannerEmails() {
        // The upcoming Monday: running at 17:00 on Sunday, the useful plan is next
        // week's, not the one that ends in a few hours.
        LocalDate monday = LocalDate.now().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        LOG.info("Sending weekly planner emails for week of " + monday);

        List<User> users = userService.findAll();
        int sent = 0;
        int skipped = 0;

        for (User user : users) {
            if (user.getEmail() == null || user.getEmail().isBlank()) {
                skipped++;
                continue;
            }
            try {
                // getOrCreateWeekPlan persists on first call, so the planner page and this
                // email show the same week rather than two independently generated ones.
                List<PlannedOutfit> stored = weeklyPlannerService.getOrCreateWeekPlan(user, monday);
                sendEmail(user, toDayArray(stored, monday), monday);
                sent++;
            } catch (Exception e) {
                LOG.log(Level.WARNING, "Failed to send weekly plan email to " + user.getEmail(), e);
            }
        }

        LOG.info("Weekly planner emails: sent=" + sent + ", skipped (no email)=" + skipped);
    }

    /** Spreads the stored plan rows across a Monday-to-Sunday array, leaving gaps null. */
    private Outfit[] toDayArray(List<PlannedOutfit> stored, LocalDate monday) {
        Outfit[] days = new Outfit[7];
        for (PlannedOutfit plan : stored) {
            int index = (int) java.time.temporal.ChronoUnit.DAYS.between(monday, plan.getPlanDate());
            if (index >= 0 && index < 7) {
                days[index] = plan.getOutfit();
            }
        }
        return days;
    }

    private void sendEmail(User user, Outfit[] plan, LocalDate monday) throws MessagingException {
        if (mailSession == null) {
            LOG.warning("Mail session not configured — skipping email for " + user.getEmail());
            return;
        }

        // The sender comes from the mail session's "from" attribute (mail.from), so
        // each deployment can use an address its SMTP provider accepts.
        String from = mailSession.getProperty("mail.from");
        MimeMessage msg = new MimeMessage(mailSession);
        msg.setFrom(new InternetAddress(from != null && !from.isBlank() ? from : DEFAULT_FROM, false));
        msg.setRecipient(Message.RecipientType.TO, new InternetAddress(user.getEmail()));
        msg.setSubject("Your Trousseau Weekly Outfit Plan — Week of " + monday.format(DATE_FMT));
        msg.setContent(buildEmailBody(user, plan, monday), "text/html; charset=UTF-8");

        Transport.send(msg);
    }

    private String buildEmailBody(User user, Outfit[] plan, LocalDate monday) {
        StringBuilder html = new StringBuilder();
        html.append("<!DOCTYPE html><html><head><meta charset='UTF-8'>")
            .append("<style>")
            .append("body{font-family:Inter,Helvetica,Arial,sans-serif;color:#1e293b;background:#f8fafc;margin:0;padding:0}")
            .append(".container{max-width:600px;margin:0 auto;padding:24px}")
            .append(".header{background:linear-gradient(135deg,#6366f1,#818cf8);padding:24px;border-radius:12px;color:white;margin-bottom:24px}")
            .append(".header h1{margin:0;font-size:22px}")
            .append(".header p{margin:4px 0 0;opacity:.85;font-size:14px}")
            .append(".day{background:white;border-radius:10px;padding:16px;margin-bottom:12px;border:1px solid #e2e8f0}")
            .append(".day-header{font-weight:700;font-size:13px;text-transform:uppercase;letter-spacing:.05em;color:#6366f1;margin-bottom:6px}")
            .append(".outfit-name{font-size:16px;font-weight:600;margin-bottom:6px}")
            .append(".badge{display:inline-block;padding:2px 8px;border-radius:9999px;font-size:12px;font-weight:600;margin-right:4px}")
            .append(".badge-occasion{background:#ede9fe;color:#5b21b6}")
            .append(".badge-season{background:#f0fdf4;color:#16a34a}")
            .append(".no-outfit{color:#94a3b8;font-style:italic}")
            .append(".footer{text-align:center;color:#94a3b8;font-size:12px;margin-top:24px}")
            .append("</style></head><body><div class='container'>")
            .append("<div class='header'>")
            .append("<h1>Your Weekly Outfit Plan</h1>")
            .append("<p>Week of ").append(monday.format(DATE_FMT))
            .append(" &mdash; ").append(monday.plusDays(6).format(DATE_FMT)).append("</p>")
            .append("</div>");

        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        for (int i = 0; i < 7; i++) {
            LocalDate date = monday.plusDays(i);
            Outfit outfit = (plan != null && i < plan.length) ? plan[i] : null;

            html.append("<div class='day'>")
                .append("<div class='day-header'>").append(days[i])
                .append(" <span style='font-weight:400;color:#64748b;font-size:12px'>")
                .append(date.format(DateTimeFormatter.ofPattern("MMM d"))).append("</span></div>");

            if (outfit != null) {
                html.append("<div class='outfit-name'>").append(escapeHtml(outfit.getName())).append("</div>");
                html.append("<div>");
                if (outfit.getOccasion() != null && !outfit.getOccasion().isBlank()) {
                    html.append("<span class='badge badge-occasion'>").append(escapeHtml(outfit.getOccasion())).append("</span>");
                }
                for (String season : outfit.getSeasons()) {
                    html.append("<span class='badge badge-season'>").append(escapeHtml(season)).append("</span>");
                }
                html.append("</div>");
                if (!outfit.getItems().isEmpty()) {
                    html.append("<p style='margin:8px 0 0;font-size:13px;color:#64748b'>")
                        .append(outfit.getItems().size()).append(" item(s): ");
                    for (int j = 0; j < outfit.getItems().size(); j++) {
                        if (j > 0) html.append(", ");
                        html.append(escapeHtml(outfit.getItems().get(j).getName()));
                    }
                    html.append("</p>");
                }
            } else {
                html.append("<span class='no-outfit'>No outfit assigned</span>");
            }

            html.append("</div>");
        }

        html.append("<div class='footer'>Trousseau &mdash; Your Smart Wardrobe Manager<br>")
            .append("You are receiving this because you have a Trousseau account.</div>")
            .append("</div></body></html>");

        return html.toString();
    }

    private String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }
}
