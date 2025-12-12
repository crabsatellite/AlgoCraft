package com.crabmods.algocraft.logic;

public class SubmissionRecord {
    public long timestamp;
    public String problemId;
    public String problemTitle;
    public String status;
    public long executionTime;
    public int passedCount;
    public int totalCount;

    public SubmissionRecord(long timestamp, String problemId, String problemTitle, String status, long executionTime, int passedCount, int totalCount) {
        this.timestamp = timestamp;
        this.problemId = problemId;
        this.problemTitle = problemTitle;
        this.status = status;
        this.executionTime = executionTime;
        this.passedCount = passedCount;
        this.totalCount = totalCount;
    }
}
