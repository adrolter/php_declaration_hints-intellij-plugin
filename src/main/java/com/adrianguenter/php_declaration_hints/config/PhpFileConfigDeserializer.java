package com.adrianguenter.php_declaration_hints.config;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

public class PhpFileConfigDeserializer
    implements JsonDeserializer<PhpFileConfig> {

    @Override
    public PhpFileConfig deserialize(
        JsonElement json,
        Type typeOfT,
        JsonDeserializationContext context
    ) throws JsonParseException {
        var jsonObject = json.getAsJsonObject();

        return new PhpFileConfig(
            jsonObject.has("autoDelete")
                && jsonObject.get("autoDelete").getAsBoolean(),
            jsonObject.has("classes")
                ? context.deserialize(jsonObject.get("classes"), PhpClassConfigMap.class)
                : new PhpClassConfigMap()
        );
    }
}
