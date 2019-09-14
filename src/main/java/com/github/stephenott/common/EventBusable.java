package com.github.stephenott.common;

import io.vertx.core.json.JsonObject;

public interface EventBusable {

    default JsonObject toJsonObject(){
        return JsonObject.mapFrom(this);
    }

}
