package com.examprep.service;

import com.examprep.dao.QuestionDao;
import com.examprep.model.Question;
import com.examprep.support.DatabaseTestSupport;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
