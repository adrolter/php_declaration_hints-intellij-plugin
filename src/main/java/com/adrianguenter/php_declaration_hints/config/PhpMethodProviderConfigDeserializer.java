package com.adrianguenter.php_declaration_hints.config;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.jetbrains.php.lang.psi.elements.PhpModifier;

import java.lang.reflect.Type;

public class PhpMethodProviderConfigDeserializer
        implements JsonDeserializer<PhpMethodProviderConfig> {
    @Override
    public PhpMethodProviderConfig deserialize(
            JsonElement json,
            Type typeOfT,
            JsonDeserializationContext context
    ) throws JsonParseException {
        var jsonObject = json.getAsJsonObject();

        return new PhpMethodProviderConfig(
                jsonObject.has("isStatic")
                        && jsonObject.get("isStatic").getAsBoolean(),
                jsonObject.has("accessLevel")
                        ? context.deserialize(jsonObject.get("accessLevel"), PhpModifier.Access.class)
                        : PhpModifier.Access.PUBLIC,
                jsonObject.has("returnType")
                        ? jsonObject.get("returnType").getAsString()
                        : "void",
                jsonObject.has("params")
                        ? context.deserialize(jsonObject.get("params"), PhpFunctionParamConfigMap.class)
                        : new PhpFunctionParamConfigMap(),
                jsonObject.has("impl")
                        ? jsonObject.get("impl").getAsString()
                        : null,
                jsonObject.has("comment")
                        ? jsonObject.get("comment").getAsString()
                        : null,
                jsonObject.has("priority")
                        ? jsonObject.get("priority").getAsDouble()
                        : null
        );
    }
}
