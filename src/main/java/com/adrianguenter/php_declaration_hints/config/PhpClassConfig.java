package com.adrianguenter.php_declaration_hints.config;

import org.jetbrains.annotations.NotNull;

public record PhpClassConfig(
        @NotNull PhpMethodProviderConfigMap methodProviders
) {
}
