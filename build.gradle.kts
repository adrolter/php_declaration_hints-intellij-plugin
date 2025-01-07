import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    id("idea")
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.intellij.platform") version "2.1.0"
}

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
//        intellijIdeaUltimate("2024.3")
//        plugins(providers.gradleProperty("platformPlugins").map { it.split(',') })
        phpstorm("2024.3")
        bundledPlugins(providers.gradleProperty("platformBundledPlugins").map { it.split(',') })
        pluginVerifier()
        zipSigner()
        instrumentationTools()
    }
}

intellijPlatform {
    instrumentCode = true
    projectName = project.name
    pluginConfiguration {
        version = getVersion()
        changeNotes = getChangeNotes().ifEmpty { "Everything! ✨" }
        description = getDescription()
    }
    signing {
        certificateChain = System.getenv("CERTIFICATE_CHAIN")
        privateKey = System.getenv("PRIVATE_KEY")
        password = System.getenv("PRIVATE_KEY_PASSWORD")
    }
    publishing {
        token = System.getenv("PUBLISH_TOKEN")
    }
    pluginVerification {
        // ...
    }
}

tasks {
    providers.gradleProperty("javaVersion").map {
        withType<JavaCompile> {
            sourceCompatibility = it
            targetCompatibility = it
        }
        withType<KotlinCompile> {
            kotlinOptions.jvmTarget = it
        }
    }

    patchPluginXml {
        sinceBuild = providers.gradleProperty("pluginSinceBuild")
        untilBuild = providers.gradleProperty("pluginUntilBuild").orNull
    }

    wrapper {
        gradleVersion = providers.gradleProperty("gradleVersion").get()
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(providers.gradleProperty("javaVersion").get()))
    }
}

fun getGithubUrl(): String {
    return "https://github.com/adrolter/php_declaration_hints-intellij-plugin"
}

fun getVersion(): String {
    return providers.gradleProperty("pluginReleaseVersion")
        .getOrElse("dev-${getGitCommitHash()}-${LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)}")
}

fun getDescription(): String {
    return """
        Provides declaration/implementation completions for PHP class methods, driven by JSON configuration files.<br>
        """
}

fun getGitCommitHash(): String {
    return providers.exec {
        commandLine("git", "rev-parse", "--short=6", "HEAD")
    }.standardOutput.asText.get().trim()
}

fun getChangeNotes(): String {
    val githubUrl = getGithubUrl()

    return providers.exec {
        commandLine(
            "/bin/sh",
            "-c",
            "git log \"\$(git tag --list | grep -E '^v[0-9]+\\.[0-9]+(\\.[0-9]+)?' |  sort -V | tail -n2 | tr '\\n' ' ' | awk '{print \$1\"..\"\$2}')\"  --no-merges --oneline --pretty=format:\"<li><a href='$githubUrl/commit/%H'>%h</a> %s <i style='color: gray;'>— %an</i></li>\""
        )
    }.standardOutput.asText.get().trim()
}
