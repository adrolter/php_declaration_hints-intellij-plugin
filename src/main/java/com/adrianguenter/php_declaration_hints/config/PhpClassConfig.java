package com.adrianguenter.php_declaration_hints.config;

import java.util.LinkedHashMap;

public record PhpClassConfig(
        LinkedHashMap<String, MethodProviderConfig> methodProviders
) {
}
