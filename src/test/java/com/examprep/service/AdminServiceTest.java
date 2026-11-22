package com.examprep.service;

import com.examprep.dao.QuestionDao;
import com.examprep.model.Exam;
import com.examprep.model.Question;
import com.examprep.model.Subject;
import com.examprep.support.DatabaseTestSupport;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminServiceTest extends DatabaseTestSupport {

    private final AdminService adminService = new AdminService();
    private final QuestionDao questionDao = new QuestionDao();

    @Test
    void deleteQuestionsRemovesSelectedAndDedupes() throws Exception {
        Question first = createSample("Batch delete first");
        Question second = createSample("Batch delete second");

        int deleted = adminService.deleteQuestions(List.of(first.getId(), first.getId(), second.getId()));

        assertEquals(2, deleted);
        assertTrue(questionDao.findById(first.getId()).isEmpty());
        assertTrue(questionDao.findById(second.getId()).isEmpty());
    }

    @Test
    void deleteQuestionsRequiresAtLeastOneId() {
        assertThrows(IllegalArgumentException.class, () -> adminService.deleteQuestions(List.of()));
        assertThrows(IllegalArgumentException.class, () -> adminService.deleteQuestions(null));
    }

    @Test
    void managesSubjectsQuestionsAndExamForms() throws Exception {
        Subject created = adminService.createSubject("Logic", "Reasoning", true, false);
        assertTrue(adminService.getAllSubjects().stream().anyMatch(s -> s.getId().equals(created.getId())));
        assertEquals("Logic", adminService.getSubject(created.getId()).orElseThrow().getName());
        adminService.updateSubject(created.getId(), "Logic II", "Updated", false, true);
        assertEquals("Logic II", adminService.getSubject(created.getId()).orElseThrow().getName());
        adminService.deleteSubject(created.getId());
        assertTrue(adminService.getSubject(created.getId()).isEmpty());

        Question question = createSample("Filter me");
        assertTrue(adminService.getQuestions(1L, QuestionImportService.UNLABELED_FILTER).stream()
                .anyMatch(q -> q.getId().equals(question.getId())));
        question.setBatchLabel("batch-a");
        adminService.updateQuestion(question);
        assertTrue(adminService.getBatchLabels().contains("batch-a"));
        assertTrue(adminService.getQuestions(1L, "batch-a").stream().anyMatch(q -> q.getId().equals(question.getId())));
        assertTrue(adminService.getQuestions(1L, "  ").size() >= adminService.getQuestionsBySubject(1L).size());
        assertTrue(adminService.getAllQuestions().size() >= 1);
        assertTrue(adminService.getQuestion(question.getId()).isPresent());
        adminService.deleteQuestion(question.getId());
        assertTrue(adminService.getQuestion(question.getId()).isEmpty());

        assertThrows(IllegalArgumentException.class, () -> adminService.deleteQuestions(Arrays.asList((Long) null)));

        Question kept = createSample("Keep on form");
        Question extra = createSample("Extra on form");
        List<Question> ordered = adminService.getQuestionsForExamForm(List.of(extra.getId(), 999_999L, kept.getId()));
        assertEquals(extra.getId(), ordered.get(0).getId());
        assertEquals(kept.getId(), ordered.get(1).getId());
        assertTrue(adminService.getQuestionsForExamForm(List.of()).size() >= 2);
        assertTrue(adminService.getQuestionsForExamForm(null).size() >= 2);

        Exam diagnostic = exam("Second diagnostic", true, false, null, true);
        Exam savedDiagnostic = adminService.createExam(diagnostic, List.of(kept.getId()));
        assertTrue(savedDiagnostic.isDiagnostic());
        assertEquals(DiagnosticService.DEFAULT_QUESTIONS_PER_SUBJECT, savedDiagnostic.getQuestionsPerSubject());
        assertFalse(adminService.getExam(2L).orElseThrow().isActive());

        savedDiagnostic.setQuestionsPerSubject(0);
        savedDiagnostic.setActive(true);
        adminService.updateExam(savedDiagnostic, List.of(kept.getId()));
        assertEquals(DiagnosticService.DEFAULT_QUESTIONS_PER_SUBJECT,
                adminService.getExam(savedDiagnostic.getId()).orElseThrow().getQuestionsPerSubject());
        assertTrue(adminService.getExamQuestionIds(savedDiagnostic.getId()).isEmpty());

        Exam activeWeekly = exam("Active weekly", false, true, 2, true);
        Exam savedActiveWeekly = adminService.createExam(activeWeekly, null);
        assertFalse(adminService.getExam(3L).orElseThrow().isActive());
        adminService.deleteExam(savedActiveWeekly.getId());
        Exam restoredWeekly = adminService.getExam(3L).orElseThrow();
        restoredWeekly.setActive(true);
        adminService.updateExam(restoredWeekly, null);

        Exam weekly = exam("Second weekly", false, true, 2, false);
        Exam savedWeekly = adminService.createExam(weekly, null);
        assertTrue(savedWeekly.isWeekly());
        assertTrue(adminService.getExam(3L).orElseThrow().isActive());
        savedWeekly.setActive(true);
        savedWeekly.setQuestionsPerSubject(3);
        adminService.updateExam(savedWeekly, null);
        assertFalse(adminService.getExam(3L).orElseThrow().isActive());

        Exam practice = exam("Extra practice", false, false, null, true);
        practice.setSubjectId(1L);
        Exam savedPractice = adminService.createExam(practice, List.of(kept.getId(), extra.getId()));
        assertEquals(List.of(kept.getId(), extra.getId()), adminService.getExamQuestionIds(savedPractice.getId()));
        adminService.updateExam(savedPractice, List.of(extra.getId()));
        assertEquals(List.of(extra.getId()), adminService.getExamQuestionIds(savedPractice.getId()));
        Exam untouchedQuestions = adminService.getExam(savedPractice.getId()).orElseThrow();
        adminService.updateExam(untouchedQuestions, null);
        assertEquals(List.of(extra.getId()), adminService.getExamQuestionIds(savedPractice.getId()));

        Exam zeroPerSubject = exam("Zero per subject", true, false, 0, false);
        Exam savedZero = adminService.createExam(zeroPerSubject, List.of());
        assertEquals(DiagnosticService.DEFAULT_QUESTIONS_PER_SUBJECT, savedZero.getQuestionsPerSubject());
        savedZero.setQuestionsPerSubject(null);
        savedZero.setActive(false);
        adminService.updateExam(savedZero, null);

        Exam inactiveWeekly = exam("Inactive weekly update", false, true, 4, false);
        adminService.updateExam(adminService.createExam(inactiveWeekly, List.of()), null);
        assertTrue(adminService.getQuestions(1L, null).size() >= 1);
        Exam barePractice = exam("Bare practice", false, false, null, false);
        adminService.createExam(barePractice, null);
        adminService.createExam(exam("Empty practice", false, false, null, false), List.of());

        adminService.deleteExam(savedPractice.getId());
        assertTrue(adminService.getExam(savedPractice.getId()).isEmpty());
        assertTrue(adminService.getAllExams().size() >= 2);
    }

    private Exam exam(String title, boolean diagnostic, boolean weekly, Integer perSubject, boolean active) {
        Exam exam = new Exam();
        exam.setSubjectId(1L);
        exam.setTitle(title);
        exam.setDurationMinutes(20);
        exam.setDiagnostic(diagnostic);
        exam.setWeekly(weekly);
        exam.setQuestionsPerSubject(perSubject);
        exam.setActive(active);
        return exam;
    }

    private Question createSample(String prompt) throws Exception {
        Question question = new Question();
        question.setSubjectId(1L);
        question.setPrompt(prompt);
        question.setOptionA("A");
        question.setOptionB("B");
        question.setOptionC("C");
        question.setOptionD("D");
        question.setCorrectOption("A");
        question.setDifficulty("MEDIUM");
        return adminService.createQuestion(question);
    }
}
