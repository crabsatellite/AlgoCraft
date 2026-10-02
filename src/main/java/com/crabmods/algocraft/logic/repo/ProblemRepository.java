package com.crabmods.algocraft.logic.repo;

import com.crabmods.algocraft.logic.Problem;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for problem repositories.
 * Repositories can be local, built-in, or remote.
 */
public interface ProblemRepository {
    /**
     * Get the name of this repository.
     */
    String getName();
    
    /**
     * Get the priority of this repository. Higher priority means problems from this
     * repository will be preferred when there are duplicates.
     */
    int getPriority();
    
    /**
     * Get all problems in this repository.
     */
    List<Problem> getProblems();
    
    /**
     * Refresh the repository (reload problems from source).
     * Returns a CompletableFuture that completes when refresh is done.
     */
    CompletableFuture<Void> refresh();
}
