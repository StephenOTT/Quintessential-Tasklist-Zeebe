package com.github.stephenott.usertask.mongo;

import com.mongodb.reactivestreams.client.MongoClient;
import com.mongodb.reactivestreams.client.MongoDatabase;

public class MongoManager {

    private static MongoClient client;
    private static String databaseName = "default";

    public static MongoClient getClient() {
        return client;
    }

    public static void setClient(MongoClient client) {
        MongoManager.client = client;
    }

    public static boolean isClientSet(){
        return client != null;
    }

    public static String getDatabaseName() {
        return databaseName;
    }

    public static void setDatabaseName(String databaseName) {
        MongoManager.databaseName = databaseName;
    }

    public static MongoDatabase getDatabase(){
        return client.getDatabase(MongoManager.getDatabaseName());
    }
}
