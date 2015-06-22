package org.inveritas.lambda.lostfilm.notification.msg;

public class Tag {
    private String key;
    private String relation;
    private String value;

    public Tag() {
    }

    public Tag(String key, String relation, String value) {
        this.key = key;
        this.relation = relation;
        this.value = value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getRelation() {
        return relation;
    }

    public void setRelation(String relation) {
        this.relation = relation;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
