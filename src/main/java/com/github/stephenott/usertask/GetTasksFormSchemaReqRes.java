package com.github.stephenott.usertask;

import com.github.stephenott.common.EventBusable;

import java.util.Map;

public class GetTasksFormSchemaReqRes {

    public static class Request implements EventBusable {

        private String taskId;

        public Request() {
        }

        public String getTaskId() {
            return taskId;
        }

        public Request setTaskId(String taskId) {
            this.taskId = taskId;
            return this;
        }
    }


    public static class Response implements EventBusable {

        private String taskId;
        private String formKey;
        private Map<String, Object> schema;
        private Map<String, Object> defaultValues;

        public Response() {
        }

        public String getTaskId() {
            return taskId;
        }

        public Response setTaskId(String taskId) {
            this.taskId = taskId;
            return this;
        }

        public String getFormKey() {
            return formKey;
        }

        public Response setFormKey(String formKey) {
            this.formKey = formKey;
            return this;
        }

        public Map<String, Object> getSchema() {
            return schema;
        }

        public Response setSchema(Map<String, Object> schema) {
            this.schema = schema;
            return this;
        }

        public Map<String, Object> getDefaultValues() {
            return defaultValues;
        }

        public Response setDefaultValues(Map<String, Object> defaultValues) {
            this.defaultValues = defaultValues;
            return this;
        }
    }
}
