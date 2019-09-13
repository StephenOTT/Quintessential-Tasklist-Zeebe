package com.github.stephenott.usertask;

import com.github.stephenott.common.EventBusable;


public class DbActionResult implements EventBusable {

    private ActionResult result;
    private Object resultObject;

    public DbActionResult() {
    }

    public DbActionResult(ActionResult result, Object resultObject) {
        this.result = result;
        this.resultObject = resultObject;
    }

    public ActionResult getResult() {
        return result;
    }

    public DbActionResult setResult(ActionResult result) {
        this.result = result;
        return this;
    }

    public Object getResultObject() {
        return resultObject;
    }

    public DbActionResult setResultObject(Object resultObject) {
        this.resultObject = resultObject;
        return this;
    }

    public enum ActionResult {
        SUCCESS,
        FAIL
    }
}
