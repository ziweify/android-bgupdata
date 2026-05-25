package com.bgupdata.lottery.api;

import com.bgupdata.lottery.model.LotteryData;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * 数据采集器 - 从台湾彩票API获取开奖数据
 */
public class LotteryCollector {

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();

    /**
     * 采集最新一期开奖数据
     * API: https://api.taiwanlottery.com/TLCAPIWeB/Lottery/LatestBingoResult
     */
    public static LotteryData fetchLatest(String proxyIp) throws IOException {
        String url = "https://api.taiwanlottery.com/TLCAPIWeB/Lottery/LatestBingoResult";

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        Response response = getClient(proxyIp).newCall(request).execute();
        if (response.body() == null) return null;

        String html = response.body().string();
        JsonObject json = JsonParser.parseString(html).getAsJsonObject();
        JsonObject content = json.getAsJsonObject("content");
        JsonObject latestPost = content.getAsJsonObject("lotteryBingoLatestPost");

        int drawTerm = latestPost.get("drawTerm").getAsInt();
        JsonArray openShowOrder = latestPost.getAsJsonArray("openShowOrder");

        if (openShowOrder.size() >= 5) {
            String s1 = openShowOrder.get(0).getAsString().trim();
            String s2 = openShowOrder.get(1).getAsString().trim();
            String s3 = openShowOrder.get(2).getAsString().trim();
            String s4 = openShowOrder.get(3).getAsString().trim();
            String s5 = openShowOrder.get(4).getAsString().trim();

            LotteryData data = new LotteryData(drawTerm);
            data.setOpenData(s1 + "," + s2 + "," + s3 + "," + s4 + "," + s5 + ",1,1,1,1,1,1,1,1,1,1,1,1,1,1,1,1");
            data.setAcTime(BgBaseApi.formatTime(new Date()));
            return data;
        }

        return null;
    }

    /**
     * 按日期分页采集开奖数据
     * API: https://api.taiwanlottery.com/TLCAPIWeB/Lottery/BingoResult?openDate=yyyy-MM-dd&pageNum=x&pageSize=y
     */
    public static List<LotteryData> fetchByDate(String dateStr, int page, int size, String proxyIp) throws IOException {
        List<LotteryData> items = new ArrayList<>();

        String url = String.format(
                "https://api.taiwanlottery.com/TLCAPIWeB/Lottery/BingoResult?openDate=%s&pageNum=%d&pageSize=%d",
                dateStr, page, size);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        Response response = getClient(proxyIp).newCall(request).execute();
        if (response.body() == null) return items;

        String html = response.body().string();
        JsonObject json = JsonParser.parseString(html).getAsJsonObject();
        JsonObject content = json.getAsJsonObject("content");
        JsonArray bingoQueryResult = content.getAsJsonArray("bingoQueryResult");

        if (bingoQueryResult == null) return items;

        for (int i = 0; i < bingoQueryResult.size(); i++) {
            JsonObject bgResult = bingoQueryResult.get(i).getAsJsonObject();
            JsonArray openShowOrder = bgResult.getAsJsonArray("openShowOrder");
            int teCode = bgResult.get("bullEyeTop").getAsInt();
            int drawTerm = bgResult.get("drawTerm").getAsInt();

            StringBuilder sbOpenData = new StringBuilder();
            for (int j = 0; j < 20 && j < openShowOrder.size(); j++) {
                int num = Integer.parseInt(openShowOrder.get(j).getAsString().trim());
                if (sbOpenData.length() > 0) {
                    sbOpenData.append(String.format(",%02d", num));
                } else {
                    sbOpenData.append(String.format("%02d", num));
                }
            }
            sbOpenData.append(String.format(",%02d", teCode));

            LotteryData data = new LotteryData(drawTerm);
            data.setOpenData(sbOpenData.toString());
            data.setAcTime(BgBaseApi.formatTime(new Date()));
            items.add(data);
        }

        return items;
    }

    private static OkHttpClient getClient(String proxyIp) {
        // 如果需要代理，可在此扩展
        // 当前直连
        return client;
    }
}
