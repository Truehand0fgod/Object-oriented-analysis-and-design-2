package com.knowledgebase.model;

import java.util.UUID;

public class Tag {
    private String id;
    private String name;

    public Tag() {
        this.id = UUID.randomUUID().toString();
    }

    public Tag(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    @Override
    public String toString() {
        return name;
    }
}