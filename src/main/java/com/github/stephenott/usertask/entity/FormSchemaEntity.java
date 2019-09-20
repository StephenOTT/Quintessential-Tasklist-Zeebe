package com.github.stephenott.usertask.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.github.stephenott.usertask.json.deserializer.JsonToStringDeserializer;

import java.time.Instant;
import java.util.UUID;

public class FormSchemaEntity {

    private String Id = UUID.randomUUID().toString();

    private Instant createdAt = Instant.now();

    private String owner;

    @JsonProperty(required = true)
    private String key;

    @JsonProperty(required = true)
    private String title;

    private String description;

    @JsonProperty(required = true)
    @JsonDeserialize(using = JsonToStringDeserializer.class)
    @JsonRawValue
    private String schema;

    public FormSchemaEntity() {
    }

    public String getId() {
        return Id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getOwner() {
        return owner;
    }

    public FormSchemaEntity setOwner(String owner) {
        this.owner = owner;
        return this;
    }

    public String getKey() {
        return key;
    }

    public FormSchemaEntity setKey(String key) {
        this.key = key;
        return this;
    }

    public String getTitle() {
        return title;
    }

    public FormSchemaEntity setTitle(String title) {
        this.title = title;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public FormSchemaEntity setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getSchema() {
        return schema;
    }

    public FormSchemaEntity setSchema(String schema) {
        this.schema = schema;
        return this;
    }

}
