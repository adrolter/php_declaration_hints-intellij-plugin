package com.adrianguenter.php_declaration_hints;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Startup
        implements ProjectActivity {

    @Override
    public @Nullable Object execute(
            @NotNull Project project,
            @NotNull Continuation<? super Unit> continuation
    ) {
        final var configRepository = project.getService(ConfigRepository.class);

        configRepository.deleteInvalidJsonConfigFiles();

        new FileWatcher(project);

        return null;
    }
}
