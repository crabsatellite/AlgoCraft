package com.crabmods.algocraft.logic;

import com.crabmods.algocraft.logic.repo.ProblemRepository;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class ProblemCacheSnapshot {
    private ProblemCacheSnapshot() {
    }

    static Map<String, Problem> build(List<ProblemRepository> repositories) {
        Map<String, Problem> rebuilt = new LinkedHashMap<>();
        for (ProblemRepository repo : repositories) {
            for (Problem problem : repo.getProblems()) {
                // First repository to add a problem wins, so caller order encodes priority.
                rebuilt.putIfAbsent(problem.getId(), problem);
            }
        }
        return Collections.unmodifiableMap(rebuilt);
    }
}
