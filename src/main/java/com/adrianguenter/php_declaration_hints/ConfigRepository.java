package com.adrianguenter.php_declaration_hints;

import com.adrianguenter.php_declaration_hints.config.PhpFileConfig;
import com.google.gson.Gson;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service(Service.Level.PROJECT)
final class ConfigRepository {
    public final String jsonConfigDirPath;
    private final int projectBasePathLength;
    private final int jsonConfigDirPathLength;
    private final Map<String, PhpFileConfig> phpFileConfigs = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();
    private final String projectBasePath;
    private final VirtualFileManager virtualFileManager;
    private final Project project;

    public ConfigRepository(
            Project project
    ) {
        ///  TODO: Make configurable via settings panel
        final String relativeJsonConfigDirPath = ".idea/phpDeclarationHints";
        this.project = project;
        this.virtualFileManager = VirtualFileManager.getInstance();
        this.projectBasePath = Objects.requireNonNull(project.getBasePath());
        this.projectBasePathLength = this.projectBasePath.length();
        this.jsonConfigDirPath = this.projectBasePath + "/" + relativeJsonConfigDirPath;
        this.jsonConfigDirPathLength = this.jsonConfigDirPath.length();
    }

    public @Nullable PhpFileConfig get(
            VirtualFile phpFile
    ) {
        return this.phpFileConfigs.computeIfAbsent(
                phpFile.getPath(),
                key -> this.loadJsonConfigFileForPhpFile(phpFile)
        );
    }

    public void invalidateCacheOfJsonConfigFile(
            VirtualFile jsonConfigFile
    ) {
        var phpFilePath = this.getPhpFilePathForJsonConfigFile(jsonConfigFile);

        if (!this.phpFileConfigs.containsKey(phpFilePath)) {
            return;
        }

        this.phpFileConfigs.remove(phpFilePath);
    }

    public void deleteInvalidJsonConfigFiles() {
        VirtualFile jsonConfigDir = this.virtualFileManager.findFileByNioPath(Paths.get(this.jsonConfigDirPath));

        if (jsonConfigDir == null || !jsonConfigDir.isDirectory()) {
            return;
        }

        RecursiveJsonConfigFileDeletionHandler handleJsonConfigFile = (
                jsonConfigFile,
                self
        ) -> {
            if (jsonConfigFile.isDirectory()) {
                for (VirtualFile child : jsonConfigFile.getChildren()) {
                    self.apply(child, self);
                }

                if (jsonConfigFile.getChildren().length == 0) {
                    try {
                        jsonConfigFile.delete(this);
                    }
                    catch (Exception ignored) {
                    }
                }

                return;
            }

            if (!jsonConfigFile.getPath().endsWith(".php.json")) {
                return;
            }

            String phpFilePath = this.getPhpFilePathForJsonConfigFile(jsonConfigFile);
            VirtualFile phpFile = this.virtualFileManager.findFileByNioPath(Paths.get(phpFilePath));

            if (phpFile == null || !phpFile.exists()) {
                try {
                    var config = this.loadJsonConfigFile(jsonConfigFile);

                    if (config != null && config.autoDelete()) {
                        jsonConfigFile.delete(this);
                    }
                } catch (Exception e) {
                    /// TODO: Log or handle
                }
            }
        };

        WriteCommandAction.runWriteCommandAction(
                this.project,
                () -> {
                    for (VirtualFile jsonConfigFile : jsonConfigDir.getChildren()) {
                        handleJsonConfigFile.call(jsonConfigFile);
                    }
                }
        );
    }

    private String getJsonConfigFilePathForPhpFile(
            VirtualFile phpFile
    ) {
        return this.jsonConfigDirPath + "/" + phpFile.getPath().substring(
                this.projectBasePathLength + 1
        ) + ".json";
    }

    private String getPhpFilePathForJsonConfigFile(
            VirtualFile jsonConfigFile
    ) {
        return this.projectBasePath + "/" + jsonConfigFile.getPath().substring(
                this.jsonConfigDirPathLength + 1,
                /// Remove `.json`
                jsonConfigFile.getPath().length() - 5
        );
    }

    private PhpFileConfig loadJsonConfigFile(
            @NotNull VirtualFile jsonFile
    ) {
        if (!jsonFile.exists()) {
            throw new RuntimeException("File does not exist: "+jsonFile.getPath());
        }

        try {
            String content = new String(jsonFile.contentsToByteArray());

//            Type type = new TypeToken<Map<String, Map<String, String>>>() {}.getType();
            return this.gson.fromJson(content, PhpFileConfig.class);
        } catch (Exception ex) {
            /// TODO: Handle JSON parsing or file access errors
            return null;
        }
    }

    private PhpFileConfig loadJsonConfigFileForPhpFile(
            VirtualFile phpFile
    ) {
        String jsonPath = this.getJsonConfigFilePathForPhpFile(phpFile);
        VirtualFile jsonFile = this.virtualFileManager.findFileByNioPath(Paths.get(jsonPath));

        if (jsonFile == null || !jsonFile.exists()) {
            return null;
        }

        return this.loadJsonConfigFile(jsonFile);
    }

    @FunctionalInterface
    interface RecursiveJsonConfigFileDeletionHandler {
        void apply(VirtualFile t, RecursiveJsonConfigFileDeletionHandler self);

        default void call(VirtualFile t) {
            this.apply(t, this);
        }
    }
}
