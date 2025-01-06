package com.adrianguenter.php_declaration_hints.config;

import java.util.Map;

public record PhpFileConfig(
        boolean autoDelete,
        Map<String, PhpClassConfig> classes
) {
}
