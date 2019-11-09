package com.github.stephenott.usertask;

import com.github.stephenott.usertask.entity.FormSchemaEntity;
import io.vertx.codegen.annotations.ProxyGen;
import io.vertx.core.Vertx;

@ProxyGen
public interface FormSchemaService {

    public static FormSchemaService create(Vertx vertx){
        return new FormSchemaServiceImpl();
    }

    public static FormSchemaService createProxy(Vertx vertx, String address){
        return new FormSchemaServiceVertxEBProxy(vertx, address);
    }

    void saveFormSchema(FormSchemaEntity formSchemaEntity);

}
