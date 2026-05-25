package com.bgupdata.lottery.post;

import com.bgupdata.lottery.model.LotteryData;

/**
 * 168盘投递
 * URL格式: url?token=xxx&issue=xxx&preDrawCode=xxx&lotCode=10047
 */
public class Post168 extends PostLotteryData {

    @Override
    public PostResult post(LotteryData data) {
        String lotteryData = formatOpenData(data);
        String fullUrl = url + "?" +
                "token=KkoN4bx5Gp7ShJdj" +
                "&issue=" + data.getIssueId() +
                "&preDrawCode=" + lotteryData +
                "&lotCode=10047";
        return doGet(fullUrl);
    }
}
