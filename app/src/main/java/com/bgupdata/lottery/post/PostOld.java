package com.bgupdata.lottery.post;

import com.bgupdata.lottery.model.LotteryData;

/**
 * 蓝盘投递 (wold)
 * URL格式: url&action_no=xxx&result=xxx
 */
public class PostOld extends PostLotteryData {

    @Override
    public PostResult post(LotteryData data) {
        String lotteryData = formatOpenData(data);
        String fullUrl = url +
                "&action_no=" + data.getIssueId() +
                "&result=" + lotteryData;
        return doGet(fullUrl);
    }
}
