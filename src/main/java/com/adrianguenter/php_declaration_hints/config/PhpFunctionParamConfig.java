package com.adrianguenter.php_declaration_hints.config;

import org.jetbrains.annotations.Nullable;

public record PhpFunctionParamConfig(
        @Nullable String type,
        boolean isVariadic,
        @Nullable String defaultValue
) {
    public boolean isOptional() {
        return this.defaultValue != null;
    }
}
