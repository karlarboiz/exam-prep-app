package com.examprep.dao;

import com.examprep.model.Question;
import com.examprep.support.DatabaseTestSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class QuestionDaoTest extends DatabaseTestSupport {

    private final QuestionDao questionDao = new QuestionDao();

    @Test
    void createAndUpdatePersistOptionalImageUrl() throws Exception {
        Question question = new Question();
        question.setSubjectId(1L);
        question.setPrompt("A figure shows a right triangle. What is the hypotenuse?");
        question.setOptionA("3");
        question.setOptionB("4");
        question.setOptionC("5");
        question.setOptionD("6");
        question.setCorrectOption("C");
        question.setDifficulty("MEDIUM");
        question.setExplanation("3-4-5 triangle");
        question.setImageUrl("https://cdn.example/triangle.png");

        Question created = questionDao.create(question);
        assertEquals("https://cdn.example/triangle.png", created.getImageUrl());

        created.setImageUrl("/media/triangle.png");
        questionDao.update(created);
        assertEquals("/media/triangle.png", questionDao.findById(created.getId()).orElseThrow().getImageUrl());

        created.setImageUrl(null);
        questionDao.update(created);
        assertNull(questionDao.findById(created.getId()).orElseThrow().getImageUrl());
    }
}
