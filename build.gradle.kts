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
        version = providers.gradleProperty("pluginReleaseVersion")
            .orElse("dev-${getGitCommitHash()}-${LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)}")
        changeNotes = getChangeNotes().ifEmpty { "Everything! ✨" }
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

fun getGitCommitHash(): String {
    return providers.exec {
        commandLine("git", "rev-parse", "--short=6", "HEAD")
    }.standardOutput.asText.get().trim()
}

fun getChangeNotes(): String {
    return providers.exec {
        commandLine(
            "/bin/sh",
            "-c",
            "git log \"\$(git tag --list | grep -E '^v[0-9]+\\.[0-9]+(\\.[0-9]+)?' |  sort -V | tail -n2 | tr '\\n' ' ' | awk '{print \$1\"..\"\$2}')\"  --no-merges --oneline --pretty=format:\"<li>%h %s (%an)</li>\" | sed '\$d'"
        )
    }.standardOutput.asText.get().trim()
}
