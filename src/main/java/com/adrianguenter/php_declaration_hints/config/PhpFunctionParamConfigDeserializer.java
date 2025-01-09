package com.adrianguenter.php_declaration_hints.config;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

public class PhpFunctionParamConfigDeserializer
    implements JsonDeserializer<PhpFunctionParamConfig> {

    @Override
    public PhpFunctionParamConfig deserialize(
        JsonElement json,
        Type typeOfT,
        JsonDeserializationContext context
    ) throws JsonParseException {
        var jsonObject = json.getAsJsonObject();

        return new PhpFunctionParamConfig(
            jsonObject.has("type")
                ? jsonObject.get("type").getAsString()
                : "mixed",
            jsonObject.has("isVariadic")
                && jsonObject.get("isVariadic").getAsBoolean(),
            jsonObject.has("defaultValue")
                ? jsonObject.get("defaultValue").getAsString()
                : null
        );
    }
}
