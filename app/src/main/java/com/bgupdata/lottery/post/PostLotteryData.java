package com.bgupdata.lottery.post;

import com.bgupdata.lottery.model.LotteryData;
import com.bgupdata.lottery.model.PostSiteType;

import java.io.IOException;
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
     * 处理opendata格式：空格分隔转逗号分隔
     */
    protected String formatOpenData(LotteryData data) {
        if (data.getOpenData() == null || data.getOpenData().isEmpty()) {
            return "";
        }
        String openData = data.getOpenData().trim();
        if (openData.contains(" ")) {
            String[] items = openData.split("\\s+");
            StringBuilder sb = new StringBuilder();
            for (String item : items) {
                if (!item.isEmpty()) {
                    if (sb.length() > 0) sb.append(",");
                    sb.append(item);
                }
            }
            return sb.toString();
        }
        return openData;
    }
}
