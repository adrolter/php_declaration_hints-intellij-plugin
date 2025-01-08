package com.adrianguenter.php_declaration_hints.config;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record PhpFunctionParamConfig(
        @NotNull String type,
        @NotNull Boolean isVariadic,
        @Nullable String defaultValue
) {
    public boolean isOptional() {
        return this.defaultValue != null;
    }
}
