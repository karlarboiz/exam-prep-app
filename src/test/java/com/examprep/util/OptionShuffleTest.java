package com.examprep.util;

import com.examprep.model.Question;
import com.examprep.model.QuestionOption;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OptionShuffleTest {

    @Test
    void shuffledOrderIsStableForTheSameAttempt() {
        Question question = sampleQuestion(7L);
        List<String> first = letters(OptionShuffle.shuffled(question, 42L));
        List<String> second = letters(OptionShuffle.shuffled(question, 42L));
        assertEquals(first, second);
    }

    @Test
    void shuffledOptionsKeepOriginalLettersAndText() {
        Question question = sampleQuestion(3L);
        List<QuestionOption> shuffled = OptionShuffle.shuffled(question, 9L);
        assertEquals(4, shuffled.size());
        assertEquals(Set.of("A", "B", "C", "D"),
                shuffled.stream().map(QuestionOption::getLetter).collect(Collectors.toSet()));
        for (QuestionOption option : shuffled) {
            assertEquals(question.getOptionText(option.getLetter()), option.getText());
        }
    }

    @Test
    void differentAttemptsCanChangeDisplayOrder() {
        Question question = sampleQuestion(11L);
        List<String> canonical = List.of("A", "B", "C", "D");
        boolean foundShuffled = false;
        List<String> otherAttemptOrder = null;
        for (long attemptId = 1; attemptId <= 48; attemptId++) {
            List<String> order = letters(OptionShuffle.shuffled(question, attemptId));
            if (!canonical.equals(order)) {
                foundShuffled = true;
            }
            if (otherAttemptOrder == null) {
                otherAttemptOrder = order;
            } else if (!otherAttemptOrder.equals(order)) {
                assertNotEquals(otherAttemptOrder, order);
                assertTrue(foundShuffled);
                return;
            }
        }
        assertTrue(foundShuffled, "expected at least one attempt to reorder options");
    }

    @Test
    void applyForAttemptSetsDisplayOptions() {
        Question question = sampleQuestion(5L);
        OptionShuffle.applyForAttempt(List.of(question), 21L);
        assertEquals(letters(OptionShuffle.shuffled(question, 21L)), letters(question.getDisplayOptions()));
    }

    private static Question sampleQuestion(Long id) {
        Question question = new Question();
        question.setId(id);
        question.setOptionA("Alpha");
        question.setOptionB("Bravo");
        question.setOptionC("Charlie");
        question.setOptionD("Delta");
        question.setCorrectOption("C");
        return question;
    }

    private static List<String> letters(List<QuestionOption> options) {
        return options.stream().map(QuestionOption::getLetter).toList();
    }
}
