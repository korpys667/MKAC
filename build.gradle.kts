import net.minecrell.pluginyml.bukkit.BukkitPluginDescription.Permission
import org.gradle.api.file.DuplicatesStrategy
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import tasks.PrintFilePathTask
import versioning.BuildConfig

plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm") version "2.2.0"
  id("com.gradleup.shadow") version "9.6.1"
  id("net.minecrell.plugin-yml.bukkit") version "0.6.0"
  id("com.diffplug.spotless") version "8.8.0"
  id("io.gitlab.arturbosch.detekt") version "1.23.8"
}

BuildConfig.init(project)

group = "ru.korpys667"

version = (findProperty("mkacVersion") as? String)?.takeIf { it.isNotBlank() } ?: "2.0.0"

val packetEventsSpigot = "com.github.retrooper:packetevents-spigot:2.13.0"

repositories {
  mavenCentral()
  maven("https://jitpack.io")
  maven("https://repo.papermc.io/repository/maven-public/")
  maven("https://repo.codemc.io/repository/maven-releases/")
  maven("https://repo.codemc.io/repository/maven-snapshots/")
  maven("https://maven.enginehub.org/repo/") // WorldGuard
  maven("https://repo.extendedclip.com/releases/") // PlaceholderAPI
  maven("https://repo.opencollab.dev/maven-snapshots/") // Geyser / Floodgate
}

dependencies {
  // Bukkit APIs
  compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
  compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.17")
  compileOnly("me.clip:placeholderapi:2.12.3")
  compileOnly("org.geysermc.floodgate:api:2.0-SNAPSHOT") {
    // Transitive org.geysermc:common no longer exists on any public repository
    // and is not required to compile against the Floodgate API.
    exclude(group = "org.geysermc", module = "common")
  }

  // PacketEvents
  if (BuildConfig.shadePE) {
    implementation(packetEventsSpigot)
  } else {
    compileOnly(packetEventsSpigot)
  }
  implementation("org.bstats:bstats-bukkit:3.2.1")

  // Cloud Command Framework
  implementation("org.incendo:cloud-core:2.1.0")
  implementation("org.incendo:cloud-paper:2.0.1") {
    exclude(group = "org.incendo", module = "cloud-core")
  }
  implementation("org.incendo:cloud-processors-requirements:1.0.0-rc.1") {
    exclude(group = "org.incendo", module = "cloud-core")
  }
  implementation("org.incendo:cloud-kotlin-extensions:2.1.0")
  implementation("org.incendo:cloud-kotlin-coroutines:2.1.0")

  // Adventure & MiniMessage
  implementation("net.kyori:adventure-platform-bukkit:4.4.1")
  implementation("net.kyori:adventure-text-minimessage:4.26.1")
  implementation("net.kyori:adventure-text-serializer-plain:4.26.1")
  implementation("net.kyori:adventure-text-serializer-gson:4.26.1")

  // HikariCP
  implementation("com.zaxxer:HikariCP:7.1.0")
  implementation("org.slf4j:slf4j-jdk14:2.0.18")
  implementation("org.jetbrains.exposed:exposed-core:1.3.1")
  implementation("org.jetbrains.exposed:exposed-java-time:1.3.1")
  implementation("org.jetbrains.exposed:exposed-jdbc:1.3.1")
  implementation("org.mariadb.jdbc:mariadb-java-client:3.5.9")
  implementation("com.fasterxml.jackson.core:jackson-databind:2.22.1")

  // Redis (cross-server alerts). Netty stays unbundled and unrelocated: PacketEvents reflects on
  // the server's io.netty Channel type, so Lettuce must share the server's Netty.
  implementation("io.lettuce:lettuce-core:6.5.0.RELEASE") { exclude(group = "io.netty") }
  compileOnly("io.netty:netty-handler:4.1.113.Final")

  // Utilities
  implementation(kotlin("stdlib"))
  implementation("it.unimi.dsi:fastutil:8.5.18")
  implementation("org.jetbrains:annotations:26.1.0")
  implementation("org.spongepowered:configurate-yaml:4.2.0")
  implementation("ru.vyarus:yaml-config-updater:1.4.4")
  implementation("io.insert-koin:koin-core:4.2.2")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
  implementation("org.jetbrains.kotlinx:kotlinx-collections-immutable:0.5.1")

  // Testing
  testImplementation(kotlin("test"))
  testImplementation(packetEventsSpigot)
  testImplementation("org.junit.jupiter:junit-jupiter:6.1.2")
  testImplementation("io.mockk:mockk:1.14.11")
  testImplementation("org.testcontainers:junit-jupiter:1.21.4")
  testImplementation("org.testcontainers:mariadb:1.21.4")
  testCompileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
  testRuntimeOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
  testRuntimeOnly("org.xerial:sqlite-jdbc:3.53.2.1")
  testRuntimeOnly("io.netty:netty-handler:4.1.113.Final")
}

configurations.all {
  resolutionStrategy {
    force("org.incendo:cloud-core:2.1.0")
  }
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(21))
  disableAutoTargetJvm()
}

kotlin { jvmToolchain(21) }

tasks.withType<JavaCompile> {
  options.release.set(17)
  options.encoding = "UTF-8"
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_17)
    freeCompilerArgs.addAll("-jvm-default=enable")
  }
}

tasks.jar { archiveClassifier.set("thin") }

tasks.shadowJar {
  archiveBaseName.set(rootProject.name)
  archiveClassifier.set("")
  duplicatesStrategy = DuplicatesStrategy.EXCLUDE
  if (!BuildConfig.shadePE) {
    archiveFileName.set("${rootProject.name}-${project.version}.unbundled.jar")
  }

  minimize {
    exclude(dependency("org.slf4j:slf4j-api"))
    exclude(dependency("org.slf4j:slf4j-jdk14"))
    exclude(dependency("org.jetbrains.exposed:exposed-core"))
    exclude(dependency("org.jetbrains.exposed:exposed-jdbc"))
    exclude(dependency("org.jetbrains.exposed:exposed-java-time"))
    exclude(dependency("org.mariadb.jdbc:mariadb-java-client"))
    exclude(dependency("io.lettuce:lettuce-core"))
    exclude(dependency("io.projectreactor:reactor-core"))
    exclude(dependency("org.reactivestreams:reactive-streams"))
    exclude(dependency("net.kyori:adventure-text-serializer-gson:.*"))
  }

  mergeServiceFiles()

  if (BuildConfig.shadePE) {
    relocate("com.github.retrooper.packetevents", "ru.korpys667.libs.packetevents.api")
    relocate("io.github.retrooper.packetevents", "ru.korpys667.libs.packetevents.impl")
    relocate("net.kyori", "ru.korpys667.libs.kyori")
  }
  relocate("org.bstats", "ru.korpys667.libs.bstats")
  relocate("org.incendo", "ru.korpys667.libs.incendo")
  relocate("io.leangen.geantyref", "ru.korpys667.libs.geantyref")
  relocate("it.unimi.dsi.fastutil", "ru.korpys667.libs.fastutil")
  relocate("com.fasterxml.jackson", "ru.korpys667.libs.jackson")
  relocate("com.zaxxer", "ru.korpys667.libs.hikari")
  relocate("org.slf4j", "ru.korpys667.libs.slf4j")
  relocate("org.jetbrains.exposed", "ru.korpys667.libs.jetbrains.exposed")
  relocate("org.spongepowered.configurate", "ru.korpys667.libs.configurate")
  relocate("org.yaml.snakeyaml", "ru.korpys667.libs.snakeyaml")
  relocate("ru.vyarus.yaml.updater", "ru.korpys667.libs.yamlupdater")
  relocate("org.joml", "ru.korpys667.libs.joml")
  relocate("org.koin", "ru.korpys667.libs.koin")
  relocate("tools.jackson", "ru.korpys667.libs.tools.jackson")
  relocate("io.lettuce", "ru.korpys667.libs.lettuce")
  relocate("reactor", "ru.korpys667.libs.reactor")
  relocate("org.reactivestreams", "ru.korpys667.libs.reactivestreams")
}

tasks.register<PrintFilePathTask>("printShadowJarPath") {
  description = "Prints the absolute path of the release shadow JAR."
  group = "help"
  file.set(tasks.shadowJar.flatMap { it.archiveFile })
}

tasks.test {
  useJUnitPlatform { excludeTags("container") }
  systemProperty("line.separator", "\n")
  jvmArgs(
    "-Dline.separator=\n",
    "-XX:+EnableDynamicAgentLoading",
    "--add-opens",
    "java.base/java.lang.reflect=ALL-UNNAMED",
    "--add-opens",
    "java.base/java.lang=ALL-UNNAMED",
  )
}

val containerTest by
  tasks.register<Test>("containerTest") {
    description = "Runs container-backed integration tests."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform { includeTags("container") }
    shouldRunAfter(tasks.test)
    jvmArgs(
      "-XX:+EnableDynamicAgentLoading",
      "--add-opens",
      "java.base/java.lang.reflect=ALL-UNNAMED",
      "--add-opens",
      "java.base/java.lang=ALL-UNNAMED",
    )
  }

tasks.build { dependsOn(tasks.shadowJar) }

detekt {
  toolVersion = "1.23.8"
  buildUponDefaultConfig = true
  allRules = false
  parallel = true
  baseline = file("config/detekt/baseline.xml")
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
  jvmTarget = "17"
  exclude("**/build/**")
}

bukkit {
  name = "MKAC"
  main = "ru.korpys667.MKAC"
  version = project.version.toString()
  apiVersion = "1.13"
  authors = listOf("korpys667")
  website = "https://mkac.fun/"
  foliaSupported = true
  if (!BuildConfig.shadePE) {
    depend = listOf("packetevents")
  }
  softDepend =
    listOf(
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
      description = "Allows usage of the help command"
      default = Permission.Default.OP
    }
    register("mkac.alerts") {
      description = "Receive alerts for violations"
      default = Permission.Default.OP
    }
    register("mkac.alerts.enable-on-join") {
      description = "Automatically enables alerts on join"
      default = Permission.Default.OP
    }
    register("mkac.reload") {
      description = "Allows reloading the config"
      default = Permission.Default.OP
    }
    register("mkac.setup") {
      description = "Allows running the setup wizard"
      default = Permission.Default.OP
    }
    register("mkac.exempt") {
      description = "Keeps the checks running but never punishes the player"
      default = Permission.Default.FALSE
    }
    register("mkac.nomitigate") {
      description = "Keeps the checks running but never mitigates the player"
      default = Permission.Default.FALSE
    }
    register("mkac.mitigations") {
      description = "Allows listing who is currently mitigated and why"
      default = Permission.Default.OP
    }
    register("mkac.mitigations.clear") {
      description = "Allows resetting a player's mitigation score by hand"
      default = Permission.Default.OP
    }
    register("mkac.mitigations.alerts") {
      description = "Allows receiving an alert when a player starts being mitigated"
      default = Permission.Default.OP
    }
    register("mkac.mitigations.alerts.enable-on-join") {
      description = "Automatically enables mitigation alerts on join"
      default = Permission.Default.OP
    }
    register("mkac.disable") {
      description = "Stops every check for the player, nothing is sent to the API"
      default = Permission.Default.FALSE
    }
    register("mkac.datacollect") {
      description = "Parent permission for data collection commands"
      default = Permission.Default.OP
      children =
        listOf(
          "mkac.collect.start",
          "mkac.collect.stop",
          "mkac.collect.cancel",
          "mkac.collect.status",
        )
    }
    register("mkac.collect.start") {
      description = "Allows starting a data collection session"
      default = Permission.Default.FALSE
    }
    register("mkac.collect.stop") {
      description = "Allows stopping a data collection session"
      default = Permission.Default.FALSE
    }
    register("mkac.collect.cancel") {
      description = "Allows cancelling a data collection session without saving"
      default = Permission.Default.FALSE
    }
    register("mkac.collect.status") {
      description = "Allows viewing data collection session status"
      default = Permission.Default.FALSE
    }
    register("mkac.monitor") {
      description = "Allows usage of the monitor command"
      default = Permission.Default.OP
      children =
        listOf(
          "mkac.monitor.self",
          "mkac.monitor.list",
          "mkac.monitor.others",
          "mkac.monitor.multi",
          "mkac.monitor.output",
          "mkac.monitor.collect",
          "mkac.monitor.all",
          "mkac.monitor.suspicious",
          "mkac.monitor.auto",
        )
    }
    register("mkac.monitor.auto") {
      description = "Allows watching whoever is fighting or already suspicious"
      default = Permission.Default.FALSE
    }
    register("mkac.monitor.all") {
      description = "Allows watching every online player at once"
      default = Permission.Default.FALSE
    }
    register("mkac.monitor.suspicious") {
      description = "Allows watching the players whose AI buffer is over the threshold"
      default = Permission.Default.FALSE
    }
    register("mkac.monitor.others") {
      description = "Allows monitoring players other than yourself"
      default = Permission.Default.FALSE
    }
    register("mkac.monitor.multi") {
      description = "Allows watching more than one player at the same time"
      default = Permission.Default.FALSE
    }
    register("mkac.monitor.collect") {
      description = "Shows the data collection and inference rows in the monitor"
      default = Permission.Default.OP
    }
    register("mkac.monitor.output") {
      description = "Parent permission for monitor output selection"
      default = Permission.Default.OP
      children =
        listOf(
          "mkac.monitor.output.actionbar",
          "mkac.monitor.output.bossbar",
          "mkac.monitor.output.sidebar",
          "mkac.monitor.output.chat",
          "mkac.monitor.output.tablist",
        )
    }
    register("mkac.monitor.output.actionbar") {
      description = "Allows using the action bar monitor output"
      default = Permission.Default.FALSE
    }
    register("mkac.monitor.output.bossbar") {
      description = "Allows using the boss bar monitor output"
      default = Permission.Default.FALSE
    }
    register("mkac.monitor.output.sidebar") {
      description = "Allows using the scoreboard sidebar monitor output"
      default = Permission.Default.FALSE
    }
    register("mkac.monitor.output.chat") {
      description = "Allows using the chat monitor output"
      default = Permission.Default.FALSE
    }
    register("mkac.monitor.output.tablist") {
      description = "Allows using the tab list monitor output"
      default = Permission.Default.FALSE
    }
    register("mkac.monitor.self") {
      description = "Allows enabling the monitor display only on self"
      default = Permission.Default.FALSE
    }
    register("mkac.monitor.list") {
      description = "Allows listing active monitor sessions"
      default = Permission.Default.OP
    }
    register("mkac.prob") {
      description = "Legacy alias for mkac.monitor"
      default = Permission.Default.FALSE
      children = listOf("mkac.monitor")
    }
    register("mkac.prob.self") {
      description = "Legacy alias for mkac.monitor.self"
      default = Permission.Default.FALSE
      children = listOf("mkac.monitor.self")
    }
    register("mkac.prob.list") {
      description = "Legacy alias for mkac.monitor.list"
      default = Permission.Default.FALSE
      children = listOf("mkac.monitor.list")
    }
    register("mkac.view") {
      description = "Allows toggling the AI nametag view on players"
      default = Permission.Default.OP
    }
    register("mkac.profile") {
      description = "Allows usage of the profile command"
      default = Permission.Default.OP
    }
    register("mkac.brand") {
      description = "Receive client brand notifications"
      default = Permission.Default.OP
    }
    register("mkac.brand.enable-on-join") {
      description = "Automatically enables brand notifications on join"
      default = Permission.Default.OP
    }
    register("mkac.history") {
      description = "Allows viewing a player's violation history"
      default = Permission.Default.OP
    }
    register("mkac.logs") {
      description = "Allows viewing recent violations"
      default = Permission.Default.OP
    }
    register("mkac.stats") {
      description = "Allows viewing server statistics"
      default = Permission.Default.OP
    }
    register("mkac.exempt.manage") {
      description = "Allows managing punishment exemptions for players"
      default = Permission.Default.OP
    }
    register("mkac.punish.manage") {
      description = "Allows managing player punishments"
      default = Permission.Default.OP
    }
    register("mkac.buffer.reset") {
      description = "Allows resetting the AI buffer of a player"
      default = Permission.Default.OP
    }
    register("mkac.suspicious") {
      description = "Permission for suspicious player commands"
      default = Permission.Default.OP
      children =
        listOf(
          "mkac.suspicious.alerts",
          "mkac.suspicious.list",
          "mkac.suspicious.top",
          "mkac.suspicious.flagged",
        )
    }
    register("mkac.suspicious.alerts") {
      description = "Allows toggling suspicious player alerts"
      default = Permission.Default.OP
    }
    register("mkac.suspicious.alerts.enable-on-join") {
      description = "Automatically enables suspicious alerts on join"
      default = Permission.Default.OP
    }
    register("mkac.suspicious.list") {
      description = "Allows listing suspicious players"
      default = Permission.Default.OP
    }
    register("mkac.suspicious.top") {
      description = "Allows viewing the top suspicious player"
      default = Permission.Default.OP
    }
    register("mkac.suspicious.flagged") {
      description = "Allows viewing online players with recorded flags"
      default = Permission.Default.OP
    }
  }
}

spotless {
  isEnforceCheck = true

  kotlin {
    target("src/**/*.kt")
    ktfmt().googleStyle()
  }

  kotlinGradle {
    target("*.gradle.kts")
    ktfmt().googleStyle()
  }
}
