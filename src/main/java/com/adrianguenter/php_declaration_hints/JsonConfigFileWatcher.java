package com.adrianguenter.php_declaration_hints;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class JsonConfigFileWatcher {
    public JsonConfigFileWatcher(
            Project project
    ) {
        final var cache = project.getService(ConfigCache.class);

        project.getMessageBus().connect().subscribe(
                VirtualFileManager.VFS_CHANGES,
                new BulkFileListener() {
                    @Override
                    public void after(@NotNull List<? extends VFileEvent> events) {
                        for (var event : events) {
                            var file = event.getFile();
                            if (file == null) {
                                continue;
                            }

                            var filePath = file.getPath();
                            if (!filePath.startsWith(cache.jsonConfigDirPath + "/")
                                    || !filePath.endsWith(".php.json")) {
                                continue;
                            }

                            cache.invalidateJsonConfigFile(file);
                        }
                    }
                });
    }

    public static final class StartupActivity
            implements com.intellij.openapi.startup.StartupActivity {

        @Override
        public void runActivity(@NotNull Project project) {
            new JsonConfigFileWatcher(project);
        }
    }
}
