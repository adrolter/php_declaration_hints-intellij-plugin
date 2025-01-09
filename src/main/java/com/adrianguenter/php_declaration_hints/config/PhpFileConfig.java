package com.adrianguenter.php_declaration_hints.config;

import org.jetbrains.annotations.NotNull;

public record PhpFileConfig(
    @NotNull Boolean autoDelete,
    @NotNull PhpClassConfigMap classes
) {
}
