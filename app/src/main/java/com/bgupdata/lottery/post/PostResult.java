package com.bgupdata.lottery.post;

import com.bgupdata.lottery.model.LotteryData;

/**
 * 投递结果
 */
public class PostResult {
    private int statusCode;
    private String html;

    public PostResult() {
        this.statusCode = 0;
        this.html = "";
    }

    public PostResult(int statusCode, String html) {
        this.statusCode = statusCode;
        this.html = html;
    }

    public int getStatusCode() { return statusCode; }
    public void setStatusCode(int statusCode) { this.statusCode = statusCode; }

    public String getHtml() { return html; }
    public void setHtml(String html) { this.html = html; }

    public boolean isSuccess() { return statusCode == 200; }
}
