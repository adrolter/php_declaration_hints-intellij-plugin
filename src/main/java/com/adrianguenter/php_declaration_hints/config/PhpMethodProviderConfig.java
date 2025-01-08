package com.adrianguenter.php_declaration_hints.config;

import com.jetbrains.php.lang.psi.elements.PhpModifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record PhpMethodProviderConfig(
        @NotNull Boolean isStatic,
        @NotNull PhpModifier.Access accessLevel,
        @NotNull String returnType,
        @NotNull PhpFunctionParamConfigMap params,
        @Nullable String impl,
        @Nullable String comment,
        @Nullable Double priority
) {
}
