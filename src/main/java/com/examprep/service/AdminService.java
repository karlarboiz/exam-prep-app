package com.examprep.service;

import com.examprep.dao.ExamDao;
import com.examprep.dao.QuestionDao;
import com.examprep.dao.SubjectDao;
import com.examprep.model.Exam;
import com.examprep.model.Question;
import com.examprep.model.Subject;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class AdminService {

    private final SubjectDao subjectDao = new SubjectDao();
    private final QuestionDao questionDao = new QuestionDao();
    private final ExamDao examDao = new ExamDao();

    public List<Subject> getAllSubjects() throws SQLException {
        return subjectDao.findAll();
    }

    public Optional<Subject> getSubject(Long id) throws SQLException {
        return subjectDao.findById(id);
    }

    public Subject createSubject(String name, String description) throws SQLException {
        return subjectDao.create(name, description);
    }

    public void updateSubject(Long id, String name, String description) throws SQLException {
        subjectDao.update(id, name, description);
    }

    public void deleteSubject(Long id) throws SQLException {
        subjectDao.delete(id);
    }

    public List<Question> getAllQuestions() throws SQLException {
        return questionDao.findAll();
    }

    public List<Question> getQuestionsBySubject(Long subjectId) throws SQLException {
        return questionDao.findBySubjectId(subjectId);
    }

    public Optional<Question> getQuestion(Long id) throws SQLException {
        return questionDao.findById(id);
    }

    public Question createQuestion(Question question) throws SQLException {
        return questionDao.create(question);
    }

    public void updateQuestion(Question question) throws SQLException {
        questionDao.update(question);
    }

    public void deleteQuestion(Long id) throws SQLException {
        questionDao.delete(id);
    }

    public List<Exam> getAllExams() throws SQLException {
        return examDao.findAll();
    }

    public Optional<Exam> getExam(Long id) throws SQLException {
        return examDao.findById(id);
    }

    public Exam createExam(Exam exam, List<Long> questionIds) throws SQLException {
        if (exam.isDiagnostic()) {
            if (exam.getQuestionsPerSubject() == null || exam.getQuestionsPerSubject() < 1) {
                exam.setQuestionsPerSubject(DiagnosticService.DEFAULT_QUESTIONS_PER_SUBJECT);
            }
        }
        Exam created = examDao.create(exam);
        if (exam.isDiagnostic()) {
            if (exam.isActive()) {
                examDao.deactivateOtherDiagnostics(created.getId());
            }
            return examDao.findById(created.getId()).orElse(created);
        }
        if (questionIds != null && !questionIds.isEmpty()) {
            examDao.setExamQuestions(created.getId(), questionIds);
        }
        return examDao.findById(created.getId()).orElse(created);
    }

    public void updateExam(Exam exam, List<Long> questionIds) throws SQLException {
        if (exam.isDiagnostic()) {
            if (exam.getQuestionsPerSubject() == null || exam.getQuestionsPerSubject() < 1) {
                exam.setQuestionsPerSubject(DiagnosticService.DEFAULT_QUESTIONS_PER_SUBJECT);
            }
            examDao.update(exam);
            if (exam.isActive()) {
                examDao.deactivateOtherDiagnostics(exam.getId());
            }
            examDao.setExamQuestions(exam.getId(), List.of());
            return;
        }
        examDao.update(exam);
        if (questionIds != null) {
            examDao.setExamQuestions(exam.getId(), questionIds);
        }
    }

    public void deleteExam(Long id) throws SQLException {
        examDao.delete(id);
    }

    public List<Long> getExamQuestionIds(Long examId) throws SQLException {
        return examDao.getQuestionIds(examId);
    }
}
