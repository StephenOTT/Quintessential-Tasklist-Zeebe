package com.github.stephenott.usertask.mongo;

import java.util.List;

public class MongoUtils {

    public static void ensureOnlyOneResult(List list) throws IllegalStateException{
        if (list.size() != 1){
            throw new IllegalStateException("More than 1 result returned by DB.  Expected only 1.");
        }
    }
}
