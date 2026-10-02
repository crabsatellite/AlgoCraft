package com.crabmods.algocraft.logic.repo;

/**
 * Metadata for a repository.
 */
public class RepositoryMetadata {
    public String name;
    public String url;
    public long lastUpdated;

    public RepositoryMetadata() {}

    public RepositoryMetadata(String name, String url) {
        this.name = name;
        this.url = url;
        this.lastUpdated = 0;
    }

    public RepositoryMetadata(RepositoryMetadata other) {
        this.name = other.name;
        this.url = other.url;
        this.lastUpdated = other.lastUpdated;
    }
}
