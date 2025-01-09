package com.adrianguenter.php_declaration_hints;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;

import java.util.Objects;

@Service(Service.Level.PROJECT)
public final class Paths {

    public final java.nio.file.Path projectBasePath;
    public final int projectBasePathLength;
    public final java.nio.file.Path jsonConfigBasePath;
    public final int jsonConfigBasePathLength;

    public Paths(
        Project project
    ) {
        ///  TODO: Make configurable via settings panel
        final var relativeJsonConfigDirPath = ".idea/phpDeclarationHints";

        this.projectBasePath = java.nio.file.Paths.get(Objects.requireNonNull(project.getBasePath()));
        this.projectBasePathLength = this.projectBasePath.toString().length();
        this.jsonConfigBasePath = this.projectBasePath.resolve(relativeJsonConfigDirPath);
        this.jsonConfigBasePathLength = this.jsonConfigBasePath.toString().length();
    }
}
