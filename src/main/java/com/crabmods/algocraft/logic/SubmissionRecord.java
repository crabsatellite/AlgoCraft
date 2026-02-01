package com.crabmods.algocraft.logic;

import java.util.Objects;

/**
 * Represents a single submission record for history tracking.
 * Immutable after construction.
 */
public class SubmissionRecord {
    private final long timestamp;
    private final String problemId;
    private final String problemTitle;
    private final String status;
    private final long executionTime;
    private final int passedCount;
    private final int totalCount;

    public SubmissionRecord(long timestamp, String problemId, String problemTitle, 
                           String status, long executionTime, int passedCount, int totalCount) {
        this.timestamp = timestamp;
        this.problemId = problemId != null ? problemId : "";
        this.problemTitle = problemTitle != null ? problemTitle : "Unknown";
        this.status = status != null ? status : "Unknown";
        this.executionTime = executionTime;
        this.passedCount = passedCount;
        this.totalCount = totalCount;
    }
    
    // Getters
    public long getTimestamp() {
        return timestamp;
    }

    public String getProblemId() {
        return problemId;
    }

    public String getProblemTitle() {
        return problemTitle;
    }

    public String getStatus() {
        return status;
    }

    public long getExecutionTime() {
        return executionTime;
    }

    public int getPassedCount() {
        return passedCount;
    }

    public int getTotalCount() {
        return totalCount;
    }
    
    /**
     * Check if this submission was accepted.
     */
    public boolean isAccepted() {
        return "Accepted".equalsIgnoreCase(status);
    }
    
    /**
     * Get the pass rate as a percentage (0-100).
     */
    public double getPassRate() {
        if (totalCount == 0) return 0;
        return (double) passedCount / totalCount * 100;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubmissionRecord that = (SubmissionRecord) o;
        return timestamp == that.timestamp 
            && executionTime == that.executionTime 
            && passedCount == that.passedCount 
            && totalCount == that.totalCount 
            && Objects.equals(problemId, that.problemId) 
            && Objects.equals(problemTitle, that.problemTitle) 
            && Objects.equals(status, that.status);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, problemId, problemTitle, status, 
                           executionTime, passedCount, totalCount);
    }

    @Override
    public String toString() {
        return "SubmissionRecord{" +
            "timestamp=" + timestamp +
            ", problemId='" + problemId + '\'' +
            ", problemTitle='" + problemTitle + '\'' +
            ", status='" + status + '\'' +
            ", executionTime=" + executionTime +
            ", passedCount=" + passedCount +
            ", totalCount=" + totalCount +
            '}';
    }
}
