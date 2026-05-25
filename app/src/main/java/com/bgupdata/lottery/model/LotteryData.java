package com.bgupdata.lottery.model;

public class LotteryData {
    private int issueId;
    private String openData;
    private int acCount;
    private String acTime;

    public LotteryData() {
        this.openData = "";
        this.acTime = "";
    }

    public LotteryData(int issueId) {
        this.issueId = issueId;
        this.openData = "";
        this.acCount = 0;
        this.acTime = "";
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
}
