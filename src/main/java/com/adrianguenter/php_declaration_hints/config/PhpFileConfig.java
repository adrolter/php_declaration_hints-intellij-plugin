package com.adrianguenter.php_declaration_hints.config;

import java.util.Map;

public record PhpFileConfig(
    Map<String, PhpClassConfig> classes
) {
}
