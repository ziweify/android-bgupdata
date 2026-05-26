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

    public interface CollectorLogger {
        void log(String message);
    }

    private static CollectorLogger logger;

    public static void setLogger(CollectorLogger l) {
        logger = l;
    }

    private static void log(String msg) {
        if (logger != null) logger.log(msg);
    }

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

        long startTime = System.currentTimeMillis();
        log("请求: " + url);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        Response response = getClient(proxyIp).newCall(request).execute();
        long elapsed = System.currentTimeMillis() - startTime;

        if (response.body() == null) {
            log("响应为空, 耗时: " + elapsed + "ms");
            return null;
        }

        String html = response.body().string();
        log("响应耗时: " + elapsed + "ms, 长度: " + html.length());

        JsonObject json = JsonParser.parseString(html).getAsJsonObject();
        JsonObject content = json.getAsJsonObject("content");
        JsonObject latestPost = content.getAsJsonObject("lotteryBingoLatestPost");

        int drawTerm = latestPost.get("drawTerm").getAsInt();
        JsonArray openShowOrder = latestPost.getAsJsonArray("openShowOrder");

        if (openShowOrder == null || openShowOrder.size() < 5) {
            log("openShowOrder不足5个, size=" + (openShowOrder == null ? 0 : openShowOrder.size()));
            return null;
        }

        log("期号: " + drawTerm + ", openShowOrder.size=" + openShowOrder.size()
                + ", has(bullEyeTop)=" + latestPost.has("bullEyeTop")
                + (latestPost.has("bullEyeTop") ? ", bullEyeTop=" + latestPost.get("bullEyeTop") : ""));

        StringBuilder sbOpenData = new StringBuilder();
        int count = Math.min(openShowOrder.size(), 20);
        for (int i = 0; i < count; i++) {
            String numStr = openShowOrder.get(i).getAsString().trim();
            int num = Integer.parseInt(numStr);
            if (sbOpenData.length() > 0) {
                sbOpenData.append(String.format(",%02d", num));
            } else {
                sbOpenData.append(String.format("%02d", num));
            }
        }

        if (count <= 5) {
            for (int i = count; i < 21; i++) {
                sbOpenData.append(",1");
            }
            log("旧API(5个号码), 补1填充到21位");
        } else {
            if (latestPost.has("bullEyeTop") && !latestPost.get("bullEyeTop").isJsonNull()) {
                int teCode = latestPost.get("bullEyeTop").getAsInt();
                sbOpenData.append(String.format(",%02d", teCode));
            } else {
                sbOpenData.append(",-1");
                log("警告: bullEyeTop字段不存在, 使用-1");
            }
        }

        LotteryData data = new LotteryData(drawTerm);
        data.setOpenData(sbOpenData.toString());
        data.setAcTime(BgBaseApi.formatTime(new Date()));

        log("结果: " + drawTerm + " -> " + data.getOpenData() + " (" + data.getOpenData().split(",").length + "个数)");
        return data;
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

        long startTime = System.currentTimeMillis();
        log("请求: " + url);

        Request request = new Request.Builder()
                .url(url)
                .get()
                .build();

        Response response = getClient(proxyIp).newCall(request).execute();
        long elapsed = System.currentTimeMillis() - startTime;

        if (response.body() == null) {
            log("响应为空, 耗时: " + elapsed + "ms");
            return items;
        }

        String html = response.body().string();
        log("响应耗时: " + elapsed + "ms, 长度: " + html.length());

        JsonObject json = JsonParser.parseString(html).getAsJsonObject();
        JsonObject content = json.getAsJsonObject("content");
        JsonArray bingoQueryResult = content.getAsJsonArray("bingoQueryResult");

        if (bingoQueryResult == null) {
            log("bingoQueryResult为空");
            return items;
        }

        log("返回 " + bingoQueryResult.size() + " 条数据");

        for (int i = 0; i < bingoQueryResult.size(); i++) {
            JsonObject bgResult = bingoQueryResult.get(i).getAsJsonObject();
            JsonArray openShowOrder = bgResult.getAsJsonArray("openShowOrder");
            int drawTerm = bgResult.get("drawTerm").getAsInt();

            int teCode;
            if (bgResult.has("bullEyeTop") && !bgResult.get("bullEyeTop").isJsonNull()) {
                teCode = bgResult.get("bullEyeTop").getAsInt();
            } else {
                teCode = -1;
                log("警告: 期号" + drawTerm + " bullEyeTop字段不存在, 使用-1");
            }

            StringBuilder sbOpenData = new StringBuilder();
            for (int j = 0; j < 20 && j < openShowOrder.size(); j++) {
                int num = Integer.parseInt(openShowOrder.get(j).getAsString().trim());
                if (sbOpenData.length() > 0) {
                    sbOpenData.append(String.format(",%02d", num));
                } else {
                    sbOpenData.append(String.format("%02d", num));
                }
            }
            if (teCode >= 0) {
                sbOpenData.append(String.format(",%02d", teCode));
            } else {
                sbOpenData.append(",-1");
            }

            LotteryData data = new LotteryData(drawTerm);
            data.setOpenData(sbOpenData.toString());
            data.setAcTime(BgBaseApi.formatTime(new Date()));
            items.add(data);
        }

        return items;
    }

    private static OkHttpClient getClient(String proxyIp) {
        return client;
    }
}
