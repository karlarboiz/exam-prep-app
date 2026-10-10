package com.examprep.service;

import com.examprep.model.Question;
import com.examprep.model.SubjectBand;
import org.junit.jupiter.api.Test;

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
}
