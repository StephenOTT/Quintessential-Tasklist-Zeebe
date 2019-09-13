package com.github.stephenott.common;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.MessageCodec;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;

public class GenericJsonObjectMessageCodec<T extends EventBusable> implements MessageCodec<T, T> {

    private Class<T> tClass;

    public GenericJsonObjectMessageCodec(Class<T> tClass) {
        this.tClass = tClass;
    }

    @Override
    public void encodeToWire(Buffer buffer, T t) {
        Buffer encoded = JsonObject.mapFrom(t).toBuffer();
        buffer.appendInt(encoded.length());
        buffer.appendBuffer(encoded);
    }

    @Override
    public T decodeFromWire(int pos, Buffer buffer) {
        int length = buffer.getInt(pos);
        pos += 4;
        return Json.decodeValue(buffer.slice(pos, pos + length), tClass);
    }

    @Override
    public T transform(T t) {
        return JsonObject.mapFrom(t).copy().mapTo(tClass);
    }

    @Override
    public String name() {
        return tClass.getCanonicalName();
    }

    @Override
    public byte systemCodecID() {
        return -1;
    }
}
