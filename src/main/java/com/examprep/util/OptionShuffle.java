package com.examprep.util;

import com.examprep.model.Question;
import com.examprep.model.QuestionOption;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Shuffles displayed answer choices per exam attempt so the correct option
 * is not always in the same position. Letters stay tied to the stored option
 * (A–D) so scoring and review still match the database.
 * Order is seeded by attempt + question id so a page refresh does not reshuffle.
 */
public final class OptionShuffle {

    private OptionShuffle() {
    }

    public static List<QuestionOption> shuffled(Question question, long attemptId) {
        List<QuestionOption> options = new ArrayList<>(question.getOptions());
        long questionId = question.getId() == null ? 0L : question.getId();
        Collections.shuffle(options, new Random(seed(attemptId, questionId)));
        return List.copyOf(options);
    }

    public static void applyForAttempt(List<Question> questions, long attemptId) {
        if (questions == null) {
            return;
        }
        for (Question question : questions) {
            question.setDisplayOptions(shuffled(question, attemptId));
        }
    }

    static long seed(long attemptId, long questionId) {
        return attemptId * 1_000_003L + questionId;
    }
}
