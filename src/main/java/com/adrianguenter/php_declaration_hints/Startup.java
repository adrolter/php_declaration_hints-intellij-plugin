package com.adrianguenter.php_declaration_hints;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;

public class Startup
        implements ProjectActivity {

    @Override
    public @Nullable Object execute(
            @NotNull Project project,
            @NotNull Continuation<? super Unit> continuation
    ) {
        final var configRepository = project.getService(ConfigRepository.class);
        final var paths = project.getService(Paths.class);

        if (!Files.exists(paths.jsonConfigBasePath)) {
            try {
                Files.createDirectories(paths.jsonConfigBasePath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        } else {
            configRepository.deleteInvalidJsonConfigFiles();
        }

        new FileWatcher(project);

        return null;
    }
}
