package com.adrianguenter.php_declaration_hints.config;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;

public class PhpClassConfigDeserializer
    implements JsonDeserializer<PhpClassConfig> {

    @Override
    public PhpClassConfig deserialize(
        JsonElement json,
        Type typeOfT,
        JsonDeserializationContext context
    ) throws JsonParseException {
        var jsonObject = json.getAsJsonObject();

        return new PhpClassConfig(
            jsonObject.has("methodProviders")
                ? context.deserialize(jsonObject.get("methodProviders"), PhpMethodProviderConfigMap.class)
                : new PhpMethodProviderConfigMap()
        );
    }
}
