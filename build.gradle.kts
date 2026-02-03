import com.github.jengelman.gradle.plugins.shadow.transformers.ServiceFileTransformer
import net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission

plugins {
    id("java")
    id("io.freefair.lombok") version "8.6"
    id("com.gradleup.shadow") version "9.0.0-beta6"
    id("net.minecrell.plugin-yml.bukkit") version "0.6.0"
    id("net.ltgt.errorprone") version "4.3.0"
    id("com.diffplug.spotless") version "7.2.1"
}

group = "ru.korpys667.mkac"
version = "1.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.codemc.io/repository/maven-snapshots/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.20.4-R0.1-SNAPSHOT")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.9")
    compileOnly("me.clip:placeholderapi:2.11.6")
    implementation("com.github.retrooper:packetevents-spigot:2.10.0")
    implementation("org.incendo:cloud-paper:2.0.0-beta.10")
    implementation("org.incendo:cloud-processors-requirements:1.0.0-rc.1")
    implementation("net.kyori:adventure-platform-bukkit:4.3.4")
    implementation("net.kyori:adventure-text-minimessage:4.17.0")
    implementation("com.zaxxer:HikariCP:7.0.2")
    implementation("org.slf4j:slf4j-jdk14:2.0.17")
    compileOnly("org.projectlombok:lombok:1.18.32")
    annotationProcessor("org.projectlombok:lombok:1.18.32")
    implementation("it.unimi.dsi:fastutil:8.5.15")
    implementation("org.jetbrains:annotations:24.1.0")
    implementation("com.google.flatbuffers:flatbuffers-java:25.2.10")
    implementation("com.google.code.gson:gson:2.10.1")
    errorprone("com.google.errorprone:error_prone_core:2.41.0")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.withType<JavaCompile> {
    options.release.set(17)
    options.encoding = "UTF-8"
}

tasks.shadowJar {
    archiveBaseName.set(rootProject.name)
    archiveClassifier.set("")

    minimize {
        exclude(dependency("org.slf4j:slf4j-api"))
        exclude(dependency("org.slf4j:slf4j-jdk14"))
    }

    transformers.add(ServiceFileTransformer())

    relocate("com.github.retrooper.packetevents", "ru.korpys667.mkac.libs.packetevents.api")
    relocate("io.github.retrooper.packetevents", "ru.korpys667.mkac.libs.packetevents.impl")
    relocate("net.kyori", "ru.korpys667.mkac.libs.kyori")
    relocate("com.google.gson", "ru.korpys667.mkac.libs.gson")
    relocate("org.incendo", "ru.korpys667.mkac.libs.incendo")
    relocate("io.leangen.geantyref", "ru.korpys667.mkac.libs.geantyref")
    relocate("it.unimi.dsi.fastutil", "ru.korpys667.mkac.libs.fastutil")
    relocate("com.google.flatbuffers", "ru.korpys667.mkac.libs.flatbuffers")
    relocate("com.zaxxer.hikari", "ru.korpys667.mkac.libs.hikari")
    relocate("org.slf4j", "ru.korpys667.mkac.libs.slf4j")
    relocate("org.jetbrains", "ru.korpys667.mkac.libs.jetbrains")
    relocate("org.intellij", "ru.korpys667.mkac.libs.intellij")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.withType<JavaCompile>().configureEach {
    dependsOn(tasks.spotlessApply)
}

bukkit {
    name = "MKAC"
    main = "ru.korpys667.mkac.MKAC"
    version = project.version.toString()
    apiVersion = "1.13"
    authors = listOf(
        "korpys667",
        "MillyOfficial"
    )
    website = "https://luxegrief.ru"
    softDepend = listOf(
        "ProtocolLib",
        "ProtocolSupport",
        "Essentials",
        "ViaVersion",
        "ViaBackwards",
        "ViaRewind",
        "Geyser-Spigot",
        "floodgate",
        "FastLogin",
        "PlaceholderAPI",
        "WorldGuard",
    )

    permissions {
        register("mkac.help") {
            description = "Показывает хелп"
            default = Permission.Default.OP
        }
        register("mkac.alerts") {
            description = "Получение уведомлений о нарушениях"
            default = Permission.Default.OP
        }
        register("mkac.alerts.enable-on-join") {
            description = "Автоматически включает оповещения при присоединении"
            default = Permission.Default.OP
        }
        register("mkac.menu") {
            description = "Открывает курятник"
            default = Permission.Default.OP
        }
        register("mkac.status") {
            description = "Включает голограмму над игроками"
            default = Permission.Default.OP
        }
        register("mkac.reload") {
            description = "Перезагрузка конфига"
            default = Permission.Default.OP
        }
        register("mkac.exempt") {
            description = "Исключение для всех чеков"
            default = Permission.Default.FALSE
        }
        register("mkac.prob") {
            description = "Разрешает смотреть вероятность (пробу)"
            default = Permission.Default.OP
        }
        register("mkac.profile") {
            description = "Смотреть профиль игрока"
            default = Permission.Default.OP
        }
        register("mkac.brand") {
            description = "Получение уведомлений о версии"
            default = Permission.Default.OP
        }
        register("mkac.brand.enable-on-join") {
            description = "Автоматически включает уведомления о версии при входе"
            default = Permission.Default.OP
        }
    }
}

spotless {
    isEnforceCheck = true

    java {
        importOrder()

        removeUnusedImports()

        googleJavaFormat("1.17.0")
    }
}