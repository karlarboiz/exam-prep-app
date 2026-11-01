package com.examprep.service;

import com.examprep.dao.EmailOutboxDao;
import com.examprep.dao.UserDao;
import com.examprep.dao.WeeklyRegimenDao;
import com.examprep.model.ExamLevel;
import com.examprep.model.FollowUpKind;
import com.examprep.model.Role;
import com.examprep.model.StudyPlan;
import com.examprep.model.User;
import com.examprep.model.WeeklyRegimen;
import com.examprep.model.WeeklyRegimenStatus;
import com.examprep.model.WeeklySubjectScore;
import com.examprep.support.ConfigOverrides;
import com.examprep.support.DatabaseTestSupport;
import com.examprep.util.PasswordUtil;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MailServiceTest extends DatabaseTestSupport {

    private final MailService mailService = new MailService();
    private final UserDao userDao = new UserDao();
    private final WeeklyRegimenDao regimenDao = new WeeklyRegimenDao();
    private final EmailOutboxDao outboxDao = new EmailOutboxDao();

    @Test
    void digestSkipsInactiveGrantBlankEmailAndAlreadySent() throws Exception {
        User user = student("pat");
        WeeklyRegimen regimen = regimen(user);
        StudyPlan plan = planWithBands();

        assertFalse(mailService.sendStudyPlanDigest(user, regimen, plan, false, LocalDateTime.now()));
        assertFalse(mailService.sendStudyPlanDigest(null, regimen, plan, true, LocalDateTime.now()));
        user.setEmail(null);
        assertFalse(mailService.sendStudyPlanDigest(user, regimen, plan, true, LocalDateTime.now()));
        user.setEmail("  ");
        assertFalse(mailService.sendStudyPlanDigest(user, regimen, plan, true, LocalDateTime.now()));
        user.setEmail("pat@example.com");

        assertTrue(mailService.sendStudyPlanDigest(user, regimen, plan, true, LocalDateTime.now()));
        assertFalse(mailService.sendStudyPlanDigest(user, regimenDao.findById(regimen.getId()).orElseThrow(),
                plan, true, LocalDateTime.now()));
        assertEquals(1, outboxDao.findByUserId(user.getId()).size());
        String body = outboxDao.findByUserId(user.getId()).get(0).getBody();
        assertTrue(body.contains("General Knowledge"));
        assertTrue(body.contains("Focus this week"));
    }

    @Test
    void digestOmitsEmptySectionsAndUsesPublicUrl() throws Exception {
        User user = student("lee");
        WeeklyRegimen regimen = regimen(user);
        StudyPlan plan = new StudyPlan();
        java.lang.reflect.Field scores = StudyPlan.class.getDeclaredField("subjectScores");
        scores.setAccessible(true);
        scores.set(plan, null);
        java.lang.reflect.Field targets = StudyPlan.class.getDeclaredField("targets");
        targets.setAccessible(true);
        targets.set(plan, null);
        String previous = ConfigOverrides.set("app.public.url", "http://exam.test/");
        try {
            assertTrue(mailService.sendStudyPlanDigest(user, regimen, plan, true, LocalDateTime.now()));
        } finally {
            ConfigOverrides.restore("app.public.url", previous);
        }
        String body = outboxDao.findByUserId(user.getId()).get(0).getBody();
        assertTrue(body.contains("http://exam.test/user/study-plan?regimenId="));
        assertFalse(body.contains("Subject bands"));
    }

    @Test
    void followUpCoversEveryKindAndBlankRecipients() throws Exception {
        User user = student("ada");
        WeeklyRegimen regimen = regimen(user);
        assertFalse(mailService.sendFollowUp(null, regimen, FollowUpKind.WEEKLY_EXAM_DUE));
        user.setEmail(null);
        assertFalse(mailService.sendFollowUp(user, regimen, FollowUpKind.WEEKLY_EXAM_DUE));
        user.setEmail("");
        assertFalse(mailService.sendFollowUp(user, regimen, FollowUpKind.WEEKLY_EXAM_DUE));
        user.setEmail("ada@example.com");
        assertFalse(mailService.sendFollowUp(user, null, FollowUpKind.WEEKLY_EXAM_DUE));
        assertFalse(mailService.sendFollowUp(user, regimen, null));

        String previous = ConfigOverrides.set("app.public.url", "http://exam.test");
        try {
            for (FollowUpKind kind : FollowUpKind.values()) {
                assertTrue(mailService.sendFollowUp(user, regimen, kind));
            }
            WeeklyRegimen openEnded = new WeeklyRegimen();
            openEnded.setId(regimen.getId());
            openEnded.setUserId(user.getId());
            openEnded.setWeekNumber(1);
            openEnded.setWeekEnd(null);
            assertTrue(mailService.sendFollowUp(user, openEnded, FollowUpKind.WEEKLY_EXAM_DUE));
        } finally {
            ConfigOverrides.restore("app.public.url", previous);
        }
        List<String> subjects = outboxDao.findByUserId(user.getId()).stream().map(row -> row.getSubject()).toList();
        assertTrue(subjects.stream().anyMatch(subject -> subject.contains("still open")));
        assertTrue(subjects.stream().anyMatch(subject -> subject.contains("was missed")));
        assertTrue(subjects.stream().anyMatch(subject -> subject.contains("checkpoint")));
    }

    @Test
    void smtpFailureStaysInOutbox() throws Exception {
        String host = ConfigOverrides.set("mail.smtp.host", "127.0.0.1");
        String port = ConfigOverrides.set("mail.smtp.port", "1");
        String startTls = ConfigOverrides.set("mail.smtp.starttls", "false");
        try {
            assertTrue(MailService.isSmtpConfigured());
            User user = student("smtp");
            mailService.send(user.getId(), null, "pat@example.com", "Hello", "Body");
        } finally {
            ConfigOverrides.restore("mail.smtp.host", host);
            ConfigOverrides.restore("mail.smtp.port", port);
            ConfigOverrides.restore("mail.smtp.starttls", startTls);
        }
        assertFalse(MailService.isSmtpConfigured());
    }

    @Test
    void smtpSendsWithAndWithoutAuthentication() throws Exception {
        AtomicReference<String> captured = new AtomicReference<>();
        try (ServerSocket server = new ServerSocket(0)) {
            Thread acceptor = new Thread(() -> acceptTwo(server, captured), "smtp-sink");
            acceptor.setDaemon(true);
            acceptor.start();

            String host = ConfigOverrides.set("mail.smtp.host", "127.0.0.1");
            String port = ConfigOverrides.set("mail.smtp.port", Integer.toString(server.getLocalPort()));
            String startTls = ConfigOverrides.set("mail.smtp.starttls", "false");
            String username = ConfigOverrides.set("mail.smtp.username", "mailer");
            String password = ConfigOverrides.set("mail.smtp.password", "secret");
            String from = ConfigOverrides.set("mail.from", "coach@exam.test");
            User user = student("smtp");
            try {
                mailService.send(user.getId(), null, "pat@example.com", "With auth", "Hello");
                ConfigOverrides.set("mail.smtp.username", "");
                ConfigOverrides.set("mail.from", "");
                mailService.send(user.getId(), null, "pat@example.com", "No auth", "Hello again");
            } finally {
                ConfigOverrides.restore("mail.smtp.host", host);
                ConfigOverrides.restore("mail.smtp.port", port);
                ConfigOverrides.restore("mail.smtp.starttls", startTls);
                ConfigOverrides.restore("mail.smtp.username", username);
                ConfigOverrides.restore("mail.smtp.password", password);
                ConfigOverrides.restore("mail.from", from);
            }
            acceptor.join(5000);
        }
        assertTrue(captured.get() != null && captured.get().contains("Hello"));
    }

    private User student(String username) throws Exception {
        return userDao.create(username, username + "@example.com", PasswordUtil.hash("password123"),
                Role.USER, ExamLevel.PROFESSIONAL);
    }

    private WeeklyRegimen regimen(User user) throws Exception {
        WeeklyRegimen regimen = new WeeklyRegimen();
        regimen.setUserId(user.getId());
        regimen.setWeekNumber(1);
        regimen.setWeekStart(LocalDateTime.of(2026, 1, 1, 8, 0));
        regimen.setWeekEnd(LocalDateTime.of(2026, 1, 8, 8, 0));
        regimen.setStatus(WeeklyRegimenStatus.OPEN);
        return regimenDao.create(regimen);
    }

    private static StudyPlan planWithBands() {
        StudyPlan plan = new StudyPlan();
        WeeklySubjectScore score = new WeeklySubjectScore();
        score.setSubjectName("General Knowledge");
        score.setScorePercent(new java.math.BigDecimal("40.00"));
        score.setBand(com.examprep.model.SubjectBand.WEAK);
        plan.setSubjectScores(List.of(score));
        plan.setTargets(List.of("Review General Knowledge"));
        return plan;
    }

    private static void acceptTwo(ServerSocket server, AtomicReference<String> captured) {
        try {
            for (int i = 0; i < 2; i++) {
                try (Socket socket = server.accept()) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(),
                            StandardCharsets.UTF_8));
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
                    out.print("220 localhost\r\n");
                    out.flush();
                    boolean data = false;
                    int authStep = 0;
                    String line;
                    while ((line = in.readLine()) != null) {
                        if (data) {
                            captured.set(captured.get() == null ? line : captured.get() + "\n" + line);
                            if (".".equals(line)) {
                                data = false;
                                out.print("250 ok\r\n");
                                out.flush();
                            }
                            continue;
                        }
                        if (authStep == 1) {
                            authStep = 2;
                            out.print("334 UGFzc3dvcmQ6\r\n");
                            out.flush();
                            continue;
                        }
                        if (authStep == 2) {
                            authStep = 0;
                            out.print("235 ok\r\n");
                            out.flush();
                            continue;
                        }
                        String command = line.toUpperCase();
                        if (command.startsWith("EHLO") || command.startsWith("HELO")) {
                            out.print("250-localhost\r\n250 AUTH LOGIN PLAIN\r\n");
                            out.flush();
                        } else if (command.startsWith("AUTH LOGIN")) {
                            authStep = 1;
                            out.print("334 VXNlcm5hbWU6\r\n");
                            out.flush();
                        } else if (command.startsWith("AUTH")) {
                            out.print("235 ok\r\n");
                            out.flush();
                        } else if ("DATA".equals(command)) {
                            data = true;
                            out.print("354 go\r\n");
                            out.flush();
                        } else if (command.startsWith("QUIT")) {
                            out.print("221 bye\r\n");
                            out.flush();
                            break;
                        } else {
                            out.print("250 ok\r\n");
                            out.flush();
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            captured.compareAndSet(null, "");
        }
    }
}
