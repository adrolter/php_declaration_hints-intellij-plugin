package com.adrianguenter.php_declaration_hints.config;

import com.jetbrains.php.lang.psi.elements.PhpModifier;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;

public record MethodProviderConfig(
        boolean isStatic,
        @Nullable PhpModifier.Access accessLevel,
        @Nullable String returnType,
        LinkedHashMap<String, PhpFunctionParamConfig> params,
        @Nullable String impl,
        @Nullable String comment
) {
}
