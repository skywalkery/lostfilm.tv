package org.inveritas.lambda.lostfilm.notification.msg;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class CreateNotificationMessage {
    @JsonProperty("app_id")
    private String appId;

    private EnLocalizedMessage contents;

    private EnLocalizedMessage headings;

    @JsonProperty("isAndroid")
    private boolean isAndroid;

    @JsonProperty("large_icon")
    private String largeIconUrl;

    private List<Tag> tags;

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public EnLocalizedMessage getContents() {
        return contents;
    }

    public void setContents(EnLocalizedMessage contents) {
        this.contents = contents;
    }

    public EnLocalizedMessage getHeadings() {
        return headings;
    }

    public void setHeadings(EnLocalizedMessage headings) {
        this.headings = headings;
    }

    public boolean getIsAndroid() {
        return isAndroid;
    }

    public void setIsAndroid(boolean isAndroid) {
        this.isAndroid = isAndroid;
    }

    public String getLargeIconUrl() {
        return largeIconUrl;
    }

    public void setLargeIconUrl(String largeIconUrl) {
        this.largeIconUrl = largeIconUrl;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }
}
