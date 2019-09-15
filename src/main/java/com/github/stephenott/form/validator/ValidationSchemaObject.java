package com.github.stephenott.form.validator;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public class ValidationSchemaObject {

    @JsonProperty(required = true)
    private String display;

    @JsonProperty(required = true)
    private List<Map<String, Object>> components;

    private Map<String, Object> settings;

    public ValidationSchemaObject() {
    }

    public String getDisplay() {
        return display;
    }

    public ValidationSchemaObject setDisplay(String display) {
        this.display = display;
        return this;
    }

    public List<Map<String, Object>> getComponents() {
        return components;
    }

    public ValidationSchemaObject setComponents(List<Map<String, Object>> components) {
        this.components = components;
        return this;
    }

    public Map<String, Object> getSettings() {
        return settings;
    }

    public ValidationSchemaObject setSettings(Map<String, Object> settings) {
        this.settings = settings;
        return this;
    }
}
