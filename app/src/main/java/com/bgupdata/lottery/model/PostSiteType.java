package com.bgupdata.lottery.model;

public enum PostSiteType {
    W168("w168"),
    WOLD("wold"),
    BOTER("boter"),
    NETBOTER("netboter");

    private final String value;

    PostSiteType(String value) {
        this.value = value;
    }

    public String getValue() { return value; }

    public static PostSiteType fromString(String text) {
        for (PostSiteType type : PostSiteType.values()) {
            if (type.value.equalsIgnoreCase(text)) {
                return type;
            }
        }
        return null;
    }
}
