package com.bgupdata.lottery.post;

import com.bgupdata.lottery.model.LotteryData;
import com.bgupdata.lottery.model.PostSiteType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.MediaType;
import okhttp3.RequestBody;

/**
 * 投递基类
 */
public abstract class PostLotteryData {
    protected PostSiteType siteType;
    protected String url;

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build();

    public PostSiteType getSiteType() { return siteType; }
    public void setSiteType(PostSiteType siteType) { this.siteType = siteType; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public abstract PostResult post(LotteryData data);

    protected PostResult doGet(String fullUrl) {
        PostResult result = new PostResult();
        try {
            Request request = new Request.Builder()
                    .url(fullUrl)
                    .get()
                    .build();
            Response response = client.newCall(request).execute();
            result.setStatusCode(response.code());
            if (response.body() != null) {
                result.setHtml(response.body().string());
            }
        } catch (IOException e) {
            result.setStatusCode(-1);
            result.setHtml("Error: " + e.getMessage());
        }
        return result;
    }

    protected PostResult doPost(String fullUrl, String jsonBody) {
        PostResult result = new PostResult();
        try {
            MediaType JSON = MediaType.parse("application/json; charset=utf-8");
            RequestBody body = RequestBody.create(jsonBody, JSON);
            Request request = new Request.Builder()
                    .url(fullUrl)
                    .post(body)
                    .addHeader("Authorization", "Admin")
                    .build();
            Response response = client.newCall(request).execute();
            result.setStatusCode(response.code());
            if (response.body() != null) {
                result.setHtml(response.body().string());
            }
        } catch (IOException e) {
            result.setStatusCode(-1);
            result.setHtml("Error: " + e.getMessage());
        }
        return result;
    }

    /**
     * 处理opendata格式，确保有21个数字
     * 兜底逻辑：如果只有20个数，复制最后一个作为第21个（与原C#程序一致）
     */
    protected String formatOpenData(LotteryData data) {
        if (data.getOpenData() == null || data.getOpenData().isEmpty()) {
            return "";
        }
        String openData = data.getOpenData().trim();
        String[] items;
        if (openData.contains(" ")) {
            items = openData.split("\\s+");
        } else {
            items = openData.split(",");
        }

        List<String> lstItem = new ArrayList<>();
        for (String item : items) {
            if (!item.isEmpty()) {
                lstItem.add(item);
            }
        }

        if (lstItem.size() == 20) {
            lstItem.add(lstItem.get(lstItem.size() - 1));
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lstItem.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(lstItem.get(i));
        }
        return sb.toString();
    }

    /**
     * 获取数据中的数字个数
     */
    protected int getDataCount(LotteryData data) {
        if (data.getOpenData() == null || data.getOpenData().isEmpty()) return 0;
        return data.getOpenData().split(",").length;
    }
}
