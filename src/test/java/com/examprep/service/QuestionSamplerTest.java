package com.examprep.service;

import com.examprep.model.Question;
import com.examprep.model.SubjectBand;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionSamplerTest {

    @Test
    void weakSubjectsGetMoreSlotsThanStrong() {
        Map<Long, Integer> quotas = QuestionSampler.quotas(
                List.of(1L, 2L),
                Map.of(1L, SubjectBand.WEAK, 2L, SubjectBand.STRONG),
                4,
                false);
        assertTrue(quotas.get(1L) > quotas.get(2L));
        assertEquals(8, quotas.get(1L) + quotas.get(2L));
    }

    @Test
    void finalWeekUsesEvenMix() {
        Map<Long, Integer> quotas = QuestionSampler.quotas(
                List.of(1L, 2L),
                Map.of(1L, SubjectBand.WEAK, 2L, SubjectBand.STRONG),
                4,
                true);
        assertEquals(quotas.get(1L), quotas.get(2L));
    }

    @Test
    void pickPrefersUnseenAndAvoidsLastWeekWhenPossible() {
        List<Question> pool = new ArrayList<>();
        for (long i = 1; i <= 6; i++) {
            Question q = new Question();
            q.setId(i);
            q.setDifficulty(i % 2 == 0 ? "EASY" : "MEDIUM");
            pool.add(q);
        }
        Set<Long> lastWeek = Set.of(1L, 2L);
        Set<Long> seen = Set.of(1L, 2L, 3L);
        List<Long> picked = QuestionSampler.pick(pool, 3, lastWeek, seen);
        assertEquals(3, picked.size());
        assertFalse(picked.contains(1L));
        assertFalse(picked.contains(2L));
    }

    @Test
    void weightTreatsMissingAndDevelopingAsNeutral() {
        assertEquals(2, QuestionSampler.weight(null, false));
        assertEquals(2, QuestionSampler.weight(SubjectBand.DEVELOPING, false));
        assertEquals(3, QuestionSampler.weight(SubjectBand.WEAK, false));
        assertEquals(1, QuestionSampler.weight(SubjectBand.STRONG, false));
        assertEquals(2, QuestionSampler.weight(SubjectBand.WEAK, true));
    }

    @Test
    void quotasRejectEmptyInputAndRebalanceRounding() {
        assertTrue(QuestionSampler.quotas(null, Map.of(), 4, false).isEmpty());
        assertTrue(QuestionSampler.quotas(List.of(), Map.of(), 4, false).isEmpty());
        assertTrue(QuestionSampler.quotas(List.of(1L), Map.of(), 0, false).isEmpty());

        Map<Long, Integer> over = QuestionSampler.quotas(
                List.of(1L, 2L, 3L),
                Map.of(1L, SubjectBand.WEAK, 2L, SubjectBand.WEAK, 3L, SubjectBand.STRONG),
                2,
                false);
        assertEquals(6, over.values().stream().mapToInt(Integer::intValue).sum());

        Map<Long, Integer> under = QuestionSampler.quotas(
                List.of(1L, 2L, 3L),
                Map.of(1L, SubjectBand.DEVELOPING, 2L, SubjectBand.DEVELOPING, 3L, SubjectBand.STRONG),
                2,
                false);
        assertEquals(6, under.values().stream().mapToInt(Integer::intValue).sum());
        assertTrue(under.get(1L) >= under.get(3L));
    }

    @Test
    void pickFallsThroughStaleThenLastWeekAndRejectsEmptyRequests() {
        assertTrue(QuestionSampler.pick(null, 2, Set.of(), Set.of()).isEmpty());
        assertTrue(QuestionSampler.pick(List.of(), 2, Set.of(), Set.of()).isEmpty());
        assertTrue(QuestionSampler.pick(List.of(question(1, "EASY")), 0, Set.of(), Set.of()).isEmpty());

        List<Question> pool = List.of(
                question(1, "EASY"),
                question(2, "HARD"),
                question(3, null),
                question(4, "TRICKY"));
        List<Long> fromNullSets = QuestionSampler.pick(pool, 2, null, null);
        assertEquals(2, fromNullSets.size());

        List<Long> thin = QuestionSampler.pick(pool, 4, Set.of(1L, 2L), Set.of(3L));
        assertEquals(4, thin.size());
        assertTrue(thin.contains(4L));
    }

    @Test
    void difficultyMixUsesOtherBucketWhenNamedBucketsRunOut() {
        assertTrue(QuestionSampler.sampleWithDifficultyMix(null, 1).isEmpty());
        assertTrue(QuestionSampler.sampleWithDifficultyMix(List.of(), 1).isEmpty());
        assertTrue(QuestionSampler.sampleWithDifficultyMix(List.of(question(1, "EASY")), 0).isEmpty());

        List<Long> mixed = QuestionSampler.sampleWithDifficultyMix(List.of(
                question(1, "easy"),
                question(2, "MEDIUM"),
                question(3, "HARD"),
                question(4, ""),
                question(5, "ODD")), 5);
        assertEquals(5, mixed.size());
        assertEquals(1, QuestionSampler.sampleWithDifficultyMix(List.of(
                question(8, "HARD"), question(9, "HARD")), 1).size());
    }

    @Test
    void mixStopsOnceTheCountIsFilledAndSkipsIdsAlreadyChosen() throws Exception {
        List<Question> pool = new ArrayList<>();
        for (long id = 1; id <= 6; id++) {
            pool.add(question(id, id <= 2 ? "EASY" : id <= 4 ? "MEDIUM" : "HARD"));
        }
        assertEquals(2, QuestionSampler.sampleWithDifficultyMix(pool, 2).size());
        assertEquals(2, QuestionSampler.sampleWithDifficultyMix(List.of(
                question(7, "ODD"), question(8, "ODD")), 2).size());

        Method without = QuestionSampler.class.getDeclaredMethod("without", List.class, List.class);
        without.setAccessible(true);
        Question kept = question(2, "EASY");
        @SuppressWarnings("unchecked")
        List<Question> remaining = (List<Question>) without.invoke(null, List.of(question(1, "EASY"), kept), List.of(1L));
        assertEquals(List.of(2L), remaining.stream().map(Question::getId).toList());
    }

    private static Question question(long id, String difficulty) {
        Question question = new Question();
        question.setId(id);
        question.setDifficulty(difficulty);
        return question;
    }
}
