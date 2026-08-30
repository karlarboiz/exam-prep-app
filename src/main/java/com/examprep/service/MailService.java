package com.examprep.service;

import com.examprep.config.AppConfig;
import com.examprep.dao.EmailOutboxDao;
import com.examprep.dao.WeeklyRegimenDao;
import com.examprep.model.StudyPlan;
import com.examprep.model.User;
import com.examprep.model.WeeklyRegimen;
import com.examprep.model.WeeklySubjectScore;
import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Properties;

/**
 * Writes {@code email_outbox} always. Sends via SMTP when {@code mail.smtp.host} is set.
 */
public class MailService {

    private final EmailOutboxDao outboxDao = new EmailOutboxDao();
    private final WeeklyRegimenDao regimenDao = new WeeklyRegimenDao();

    public boolean sendStudyPlanDigest(User user, WeeklyRegimen regimen, StudyPlan plan,
                                       boolean grantActive, LocalDateTime now) throws SQLException {
        if (!grantActive || user == null || user.getEmail() == null || user.getEmail().isBlank()) {
            return false;
        }
        if (regimen.getEmailSentAt() != null) {
            return false;
        }
        String subject = "Week " + regimen.getWeekNumber() + " study plan";
        String body = buildBody(user, regimen, plan);
        send(user.getId(), regimen.getId(), user.getEmail(), subject, body);
        regimenDao.markEmailSent(regimen.getId(), now);
        return true;
    }

    public void send(Long userId, Long regimenId, String toAddress, String subject, String body)
            throws SQLException {
        outboxDao.insert(userId, regimenId, toAddress, subject, body);
        if (!isSmtpConfigured()) {
            System.out.println("[mail] to=" + toAddress + " subject=" + subject + " (outbox only)");
            return;
        }
        try {
            sendSmtp(toAddress, subject, body);
            System.out.println("[mail] to=" + toAddress + " subject=" + subject + " (smtp)");
        } catch (Exception e) {
            System.err.println("[mail] SMTP send failed for " + toAddress + ": " + e.getMessage());
        }
    }

    public static boolean isSmtpConfigured() {
        String host = AppConfig.get("mail.smtp.host", "");
        return host != null && !host.isBlank();
    }

    private void sendSmtp(String toAddress, String subject, String body) throws Exception {
        String host = AppConfig.get("mail.smtp.host", "");
        String port = AppConfig.get("mail.smtp.port", "587");
        String username = AppConfig.get("mail.smtp.username", "");
        String password = AppConfig.get("mail.smtp.password", "");
        String from = AppConfig.get("mail.from", username.isBlank() ? "noreply@localhost" : username);
        boolean startTls = AppConfig.getBoolean("mail.smtp.starttls", true);

        Properties props = new Properties();
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);
        props.put("mail.smtp.starttls.enable", Boolean.toString(startTls));
        boolean auth = username != null && !username.isBlank();
        props.put("mail.smtp.auth", Boolean.toString(auth));

        Session session = auth
                ? Session.getInstance(props, new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(username, password);
                    }
                })
                : Session.getInstance(props);

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toAddress));
        message.setSubject(subject, "UTF-8");
        message.setText(body, "UTF-8");
        Transport.send(message);
    }

    private String buildBody(User user, WeeklyRegimen regimen, StudyPlan plan) {
        String base = AppConfig.get("app.public.url", "").trim();
        String link = (base.isEmpty() ? "" : base.replaceAll("/$", ""))
                + "/user/study-plan?regimenId=" + regimen.getId();
        StringBuilder sb = new StringBuilder();
        sb.append("Hi ").append(user.getUsername()).append(",\n\n");
        sb.append("Your week ").append(regimen.getWeekNumber()).append(" exam is in. ");
        sb.append("Open the app to review — this email is only a reminder.\n\n");
        if (plan.getSubjectScores() != null) {
            sb.append("Subject bands:\n");
            for (WeeklySubjectScore score : plan.getSubjectScores()) {
                sb.append("- ").append(score.getSubjectName()).append(": ")
                        .append(score.getScorePercent()).append("% (")
                        .append(score.getBand()).append(")\n");
            }
            sb.append('\n');
        }
        if (plan.getTargets() != null && !plan.getTargets().isEmpty()) {
            sb.append("Focus this week:\n");
            for (String target : plan.getTargets()) {
                sb.append("- ").append(target).append('\n');
            }
            sb.append('\n');
        }
        sb.append("Study plan: ").append(link.isBlank() ? "/user/study-plan" : link).append('\n');
        return sb.toString();
    }
}
