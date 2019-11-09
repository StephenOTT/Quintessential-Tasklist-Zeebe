package com.github.stephenott.usertask;

import com.github.stephenott.common.EventBusable;

import java.util.Collections;
import java.util.List;

public class DbActionResult implements EventBusable {

    private List<?> resultObjects;

    private DbActionResult() {
    }

    public DbActionResult(Object resultObjects){
        this.resultObjects = Collections.singletonList(resultObjects);
    }

    public DbActionResult(List<?> resultObjects) {
        this.resultObjects = resultObjects;
    }

    public List<?> getResultObjects() {
        return resultObjects;
    }

    public DbActionResult setResultObjects(List<?> resultObjects) {
        this.resultObjects = resultObjects;
        return this;
    }

}
