package com.examprep.service;

import com.examprep.config.AppConfig;
import com.examprep.dao.EmailOutboxDao;
import com.examprep.dao.WeeklyRegimenDao;
import com.examprep.model.StudyPlan;
import com.examprep.model.User;
import com.examprep.model.WeeklyRegimen;
import com.examprep.model.WeeklySubjectScore;

import java.sql.SQLException;
import java.time.LocalDateTime;

/**
 * Study-plan digest. Writes {@code email_outbox} and logs. Skip when the access grant has expired.
 * The in-app study plan remains the source of truth.
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
        outboxDao.insert(user.getId(), regimen.getId(), user.getEmail(), subject, body);
        regimenDao.markEmailSent(regimen.getId(), now);
        System.out.println("[mail] to=" + user.getEmail() + " subject=" + subject);
        return true;
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
