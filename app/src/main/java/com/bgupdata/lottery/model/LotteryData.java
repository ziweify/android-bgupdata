package com.bgupdata.lottery.model;

public class LotteryData {
    public enum Status {
        COLLECTING,
        SUCCESS,
        FAILED
    }

    private int issueId;
    private String openData;
    private int acCount;
    private String acTime;
    private Status status;

    public LotteryData() {
        this.openData = "";
        this.acTime = "";
        this.status = Status.COLLECTING;
    }

    public LotteryData(int issueId) {
        this.issueId = issueId;
        this.openData = "";
        this.acCount = 0;
        this.acTime = "";
        this.status = Status.COLLECTING;
    }

    public int getIssueId() { return issueId; }
    public void setIssueId(int issueId) { this.issueId = issueId; }

    public String getOpenData() { return openData; }
    public void setOpenData(String openData) { this.openData = openData; }

    public int getAcCount() { return acCount; }
    public void setAcCount(int acCount) { this.acCount = acCount; }
    public void incrementAcCount() { this.acCount++; }

    public String getAcTime() { return acTime; }
    public void setAcTime(String acTime) { this.acTime = acTime; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }
}
