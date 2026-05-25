package com.bgupdata.lottery.post;

import com.bgupdata.lottery.model.PostSiteType;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 投递工厂 - 解析地址配置并创建对应的投递实例
 * 地址格式: [类型]URL
 * 例如: [wold]http://xxx.xxx.xxx/api/task/upload_twbgone?token=xxx
 *       [w168]http://xxx/api/upload_result.do
 *       [boter]http://xxx:789/api/boter/uploadbg
 *       [netboter]http://xxx:5300/api/syncLotteryRecord
 */
public class PostFactory {

    private static final Pattern TYPE_PATTERN = Pattern.compile("\\[([^\\]]+)]");
    private static final Pattern URL_PATTERN = Pattern.compile("\\[[^\\]]+](.+)");

    /**
     * 解析多行地址配置，创建投递实例列表
     * @param addressConfig 多行配置文本，每行格式: [类型]URL
     */
    public static List<PostLotteryData> create(String addressConfig) {
        List<PostLotteryData> result = new ArrayList<>();
        if (addressConfig == null || addressConfig.trim().isEmpty()) {
            return result;
        }

        String[] lines = addressConfig.split("[,\\n]");
        for (String line : lines) {
            line = line.trim().replace("\r", "");
            if (line.isEmpty()) continue;

            Matcher typeMatcher = TYPE_PATTERN.matcher(line);
            Matcher urlMatcher = URL_PATTERN.matcher(line);

            if (typeMatcher.find() && urlMatcher.find()) {
                String typeStr = typeMatcher.group(1);
                String urlStr = urlMatcher.group(1).trim();

                if (urlStr.isEmpty()) continue;

                PostSiteType siteType = PostSiteType.fromString(typeStr);
                if (siteType != null) {
                    PostLotteryData poster = createPoster(siteType, urlStr);
                    if (poster != null) {
                        result.add(poster);
                    }
                }
            }
        }

        return result;
    }

    /**
     * 根据类型创建单个投递实例
     */
    public static PostLotteryData createPoster(PostSiteType siteType, String url) {
        PostLotteryData poster = null;
        switch (siteType) {
            case W168:
                poster = new Post168();
                break;
            case WOLD:
                poster = new PostOld();
                break;
            case BOTER:
                poster = new PostBoter();
                break;
            case NETBOTER:
                poster = new PostNetBoter();
                break;
        }
        if (poster != null) {
            poster.setUrl(url);
            poster.setSiteType(siteType);
        }
        return poster;
    }
}
