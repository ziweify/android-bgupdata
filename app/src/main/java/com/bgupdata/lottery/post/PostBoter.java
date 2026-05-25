package com.bgupdata.lottery.post;

import com.bgupdata.lottery.model.LotteryData;

/**
 * 机器人盘投递 (boter)
 * URL格式: url?token=xxx&issueid=xxx&lotteryCode=xxx
 */
public class PostBoter extends PostLotteryData {

    @Override
    public PostResult post(LotteryData data) {
        String lotteryData = formatOpenData(data);
        String fullUrl = url + "?" +
                "token=KkoN4bx5Gp7ShJdj" +
                "&issueid=" + data.getIssueId() +
                "&lotteryCode=" + lotteryData;
        return doGet(fullUrl);
    }
}
