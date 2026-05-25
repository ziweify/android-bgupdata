package com.bgupdata.lottery.model;

/**
 * 投递地址配置项
 */
public class AddressItem {
    private String url;
    private PostSiteType siteType;
    private boolean enabled;

    public AddressItem(String url, PostSiteType siteType, boolean enabled) {
        this.url = url;
        this.siteType = siteType;
        this.enabled = enabled;
    }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public PostSiteType getSiteType() { return siteType; }
    public void setSiteType(PostSiteType siteType) { this.siteType = siteType; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    /**
     * 转为投递配置字符串格式: [type]url
     */
    public String toConfigString() {
        return "[" + siteType.getValue() + "]" + url;
    }
}
