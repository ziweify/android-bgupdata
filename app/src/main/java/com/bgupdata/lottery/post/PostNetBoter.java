package com.bgupdata.lottery.post;

import com.bgupdata.lottery.api.BgBaseApi;
import com.bgupdata.lottery.model.LotteryData;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * 网络版机器人投递 (netboter)
 * POST JSON格式
 */
public class PostNetBoter extends PostLotteryData {

    @Override
    public PostResult post(LotteryData data) {
        long openTime = BgBaseApi.getOpenTimestamp(data.getIssueId());

        String openNum = "";
        if (data.getOpenData() != null && !data.getOpenData().isEmpty()) {
            String[] parts = data.getOpenData().split(",");
            if (parts.length >= 5) {
                openNum = parts[0] + "," + parts[1] + "," + parts[2] + "," + parts[3] + "," + parts[4];
            }
        }

        JsonObject lotteryObj = new JsonObject();
        lotteryObj.addProperty("game", 1);
        lotteryObj.addProperty("lottery_periods", data.getIssueId());
        lotteryObj.addProperty("open_time", openTime);
        lotteryObj.addProperty("open_num", openNum);

        JsonArray dataArray = new JsonArray();
        dataArray.add(lotteryObj);

        JsonObject postBody = new JsonObject();
        postBody.add("data", dataArray);

        return doPost(url, postBody.toString());
    }
}
