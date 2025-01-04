package com.adrianguenter.php_declaration_hints;

import com.adrianguenter.php_declaration_hints.config.PhpFileConfig;
import com.google.gson.Gson;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service(Service.Level.PROJECT)
final class ConfigCache {
    public final int projectBasePathLength;
    public final String relativeJsonConfigDirPath = ".idea/phpDeclarationHints";
    public final String jsonConfigDirPath;
    public final int jsonConfigDirPathLength;
    private final Map<String, PhpFileConfig> phpFileConfigs = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();
    private final String projectBasePath;
    private final VirtualFileManager virtualFileManager;

    public ConfigCache(
            Project project
    ) {
        this.virtualFileManager = VirtualFileManager.getInstance();
        this.projectBasePath = Objects.requireNonNull(project.getBasePath());
        this.projectBasePathLength = this.projectBasePath.length();
        this.jsonConfigDirPath = this.projectBasePath + "/" + this.relativeJsonConfigDirPath;
        this.jsonConfigDirPathLength = this.jsonConfigDirPath.length();
    }

    public @Nullable PhpFileConfig getPhpFileConfig(
            VirtualFile phpFile
    ) {
        return this.phpFileConfigs.computeIfAbsent(
                phpFile.getPath(),
                key -> this.loadJsonConfigFile(phpFile)
        );
    }

    public void invalidateJsonConfigFile(
            VirtualFile jsonConfigFile
    ) {
        var phpFilePath = this.getPhpFilePathForJsonConfigFile(jsonConfigFile);

        if (!this.phpFileConfigs.containsKey(phpFilePath)) {
            return;
        }

        this.phpFileConfigs.remove(phpFilePath);
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
            VirtualFile phpFile
    ) {
        String jsonPath = this.getJsonConfigFilePathForPhpFile(phpFile);
        VirtualFile jsonFile = this.virtualFileManager.findFileByNioPath(Paths.get(jsonPath));

        if (jsonFile == null || !jsonFile.exists()) {
            return null;
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
}
