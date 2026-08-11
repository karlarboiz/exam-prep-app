package com.examprep.dao;

import com.examprep.model.ExamLevel;
import com.examprep.model.Subject;
import com.examprep.support.DatabaseTestSupport;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubjectDaoTest extends DatabaseTestSupport {

    private final SubjectDao subjectDao = new SubjectDao();

    @Test
    void seedSubjectIsVisibleToBothExamLevels() throws Exception {
        List<Subject> professional = subjectDao.findByExamLevel(ExamLevel.PROFESSIONAL);
        List<Subject> subProfessional = subjectDao.findByExamLevel(ExamLevel.SUB_PROFESSIONAL);

        assertFalse(professional.isEmpty(), "seed subject should appear for PROFESSIONAL");
        assertFalse(subProfessional.isEmpty(), "seed subject should appear for SUB_PROFESSIONAL");
        assertTrue(professional.stream().anyMatch(s -> "General Knowledge".equals(s.getName())));
        assertTrue(subProfessional.stream().anyMatch(s -> "General Knowledge".equals(s.getName())));
    }
}
