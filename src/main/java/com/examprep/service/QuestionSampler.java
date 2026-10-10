package com.examprep.service;

import com.examprep.model.Question;
import com.examprep.model.SubjectBand;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Item selector for weekly forms and checkpoints. Prefers unseen items, then stale,
 * and only reuses last week's official form when the bank is too thin.
 */
public final class QuestionSampler {

    private QuestionSampler() {
    }

    public static int weight(SubjectBand band, boolean evenMix) {
        if (evenMix) {
            return 2;
        }
        if (band == SubjectBand.WEAK) {
            return 3;
        }
        if (band == SubjectBand.STRONG) {
            return 1;
        }
        return 2;
    }

    public static Map<Long, Integer> quotas(List<Long> subjectIds, Map<Long, SubjectBand> bands,
                                            int basePerSubject, boolean evenMix) {
        Map<Long, Integer> quotas = new LinkedHashMap<>();
        if (subjectIds == null || subjectIds.isEmpty() || basePerSubject < 1) {
            return quotas;
        }
        int targetTotal = basePerSubject * subjectIds.size();
        int totalWeight = 0;
        List<Integer> weights = new ArrayList<>();
        for (Long subjectId : subjectIds) {
            int w = weight(bands.get(subjectId), evenMix);
            weights.add(w);
            totalWeight += w;
        }
        int assigned = 0;
        for (int i = 0; i < subjectIds.size(); i++) {
            int quota = Math.max(1, (int) Math.round(targetTotal * (weights.get(i) / (double) totalWeight)));
            quotas.put(subjectIds.get(i), quota);
            assigned += quota;
        }
        while (assigned > targetTotal) {
            Long strongest = subjectIds.stream()
                    .filter(id -> quotas.get(id) > 1)
                    .max((a, b) -> Integer.compare(quotas.get(a), quotas.get(b)))
                    .orElse(null);
            if (strongest == null) {
                break;
            }
            quotas.put(strongest, quotas.get(strongest) - 1);
            assigned--;
        }
        while (assigned < targetTotal) {
            Long weakest = subjectIds.stream()
                    .max((a, b) -> Integer.compare(weight(bands.get(a), evenMix), weight(bands.get(b), evenMix)))
                    .orElse(subjectIds.get(0));
            quotas.put(weakest, quotas.get(weakest) + 1);
            assigned++;
        }
        return quotas;
    }

    public static List<Long> pick(List<Question> pool, int count, Set<Long> lastWeekExclude, Set<Long> seen) {
        if (pool == null || pool.isEmpty() || count < 1) {
            return List.of();
        }
        Set<Long> excludeLast = lastWeekExclude != null ? lastWeekExclude : Set.of();
        Set<Long> seenIds = seen != null ? seen : Set.of();

        List<Question> unseen = new ArrayList<>();
        List<Question> stale = new ArrayList<>();
        List<Question> lastResort = new ArrayList<>();
        for (Question q : pool) {
            if (excludeLast.contains(q.getId())) {
                lastResort.add(q);
            } else if (seenIds.contains(q.getId())) {
                stale.add(q);
            } else {
                unseen.add(q);
            }
        }

        List<Long> picked = new ArrayList<>(sampleWithDifficultyMix(unseen, count));
        if (picked.size() < count) {
            picked.addAll(sampleWithDifficultyMix(without(stale, picked), count - picked.size()));
        }
        if (picked.size() < count) {
            picked.addAll(sampleWithDifficultyMix(without(lastResort, picked), count - picked.size()));
        }
        return picked;
    }

    public static List<Long> sampleWithDifficultyMix(List<Question> pool, int count) {
        if (pool == null || pool.isEmpty() || count < 1) {
            return List.of();
        }
        int take = Math.min(count, pool.size());
        Map<String, List<Question>> byDifficulty = new LinkedHashMap<>();
        byDifficulty.put("EASY", new ArrayList<>());
        byDifficulty.put("MEDIUM", new ArrayList<>());
        byDifficulty.put("HARD", new ArrayList<>());
        List<Question> other = new ArrayList<>();
        for (Question q : pool) {
            String d = q.getDifficulty() == null ? "MEDIUM" : q.getDifficulty().toUpperCase();
            if (byDifficulty.containsKey(d)) {
                byDifficulty.get(d).add(q);
            } else {
                other.add(q);
            }
        }
        for (List<Question> list : byDifficulty.values()) {
            Collections.shuffle(list, ThreadLocalRandom.current());
        }
        Collections.shuffle(other, ThreadLocalRandom.current());

        List<Long> picked = new ArrayList<>();
        String[] order = {"EASY", "MEDIUM", "HARD"};
        int difficultyIndex = 0;
        while (picked.size() < take) {
            boolean added = false;
            for (int i = 0; i < order.length && picked.size() < take; i++) {
                String key = order[(difficultyIndex + i) % order.length];
                List<Question> bucket = byDifficulty.get(key);
                if (!bucket.isEmpty()) {
                    picked.add(bucket.remove(0).getId());
                    added = true;
                    difficultyIndex = (difficultyIndex + i + 1) % order.length;
                    break;
                }
            }
            if (!added) {
                break;
            }
        }
        for (Question q : other) {
            if (picked.size() >= take) {
                break;
            }
            picked.add(q.getId());
        }
        if (picked.size() < take) {
            List<Question> remaining = new ArrayList<>(pool);
            remaining.removeIf(q -> picked.contains(q.getId()));
            Collections.shuffle(remaining, ThreadLocalRandom.current());
            for (Question q : remaining) {
                if (picked.size() >= take) {
                    break;
                }
                picked.add(q.getId());
            }
        }
        return picked;
    }

    private static List<Question> without(List<Question> pool, List<Long> picked) {
        List<Question> remaining = new ArrayList<>();
        for (Question q : pool) {
            if (!picked.contains(q.getId())) {
                remaining.add(q);
            }
        }
        return remaining;
    }
}
