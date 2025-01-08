package com.adrianguenter.php_declaration_hints;

import com.adrianguenter.php_declaration_hints.config.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service(Service.Level.PROJECT)
final class ConfigRepository {
    private final Map<String, PhpFileConfig> phpFileConfigs = new ConcurrentHashMap<>();
    private final Gson gson;
    private final VirtualFileManager virtualFileManager;
    private final Project project;
    private final Paths paths;

    public ConfigRepository(
            Project project
    ) {
        this.project = project;
        this.paths = project.getService(Paths.class);
        this.virtualFileManager = VirtualFileManager.getInstance();

        var gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(PhpFileConfig.class, new PhpFileConfigDeserializer());
        gsonBuilder.registerTypeAdapter(PhpClassConfig.class, new PhpClassConfigDeserializer());
        gsonBuilder.registerTypeAdapter(PhpMethodProviderConfig.class, new PhpMethodProviderConfigDeserializer());
        gsonBuilder.registerTypeAdapter(PhpFunctionParamConfig.class, new PhpFunctionParamConfigDeserializer());
        this.gson = gsonBuilder.create();
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
        VirtualFile jsonConfigDir = this.virtualFileManager.findFileByNioPath(
                java.nio.file.Paths.get(this.paths.jsonConfigBasePath));

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
            VirtualFile phpFile = this.virtualFileManager.findFileByNioPath(
                    java.nio.file.Paths.get(phpFilePath));

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
        return this.paths.jsonConfigBasePath + "/" + phpFile.getPath().substring(
                this.paths.projectBasePathLength + 1
        ) + ".json";
    }

    private String getPhpFilePathForJsonConfigFile(
            VirtualFile jsonConfigFile
    ) {
        return this.paths.projectBasePath + "/" + jsonConfigFile.getPath().substring(
                this.paths.jsonConfigBasePathLength + 1,
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
        VirtualFile jsonFile = this.virtualFileManager.findFileByNioPath(
                java.nio.file.Paths.get(jsonPath));

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
