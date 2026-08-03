import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.4.10"
    kotlin("plugin.serialization") version "2.4.10" apply false
    id("net.fabricmc.fabric-loom") version "1.17-SNAPSHOT"
    id("com.gradleup.shadow") version "9.6.1"
    id("maven-publish")
}

val modVersion = providers.gradleProperty("mod_version").get()
val mavenGroup = providers.gradleProperty("maven_group").get()
val archivesBaseName = providers.gradleProperty("archives_base_name").get()
val minecraftVersion = providers.gradleProperty("minecraft_version").get()
val loaderVersion = providers.gradleProperty("loader_version").get()
val kotlinLoaderVersion = providers.gradleProperty("kotlin_loader_version").get()
val fabricVersion = providers.gradleProperty("fabric_version").get()
val moulConfigVersion = providers.gradleProperty("moulconfig_version").get()
val hypixelModApiVersion = providers.gradleProperty("hypixel_mod_api_version").get()
val hypixelModFabricVersion = providers.gradleProperty("hypixel_mod_fabric_version").get()

val shadowModImpl = configurations.create("shadowModImpl")
configurations.named("implementation") {
    extendsFrom(shadowModImpl)
}

version = "$modVersion+$minecraftVersion"
group = mavenGroup

base {
    archivesName.set(archivesBaseName)
}

val targetJavaVersion = 25
java {
    toolchain.languageVersion = JavaLanguageVersion.of(targetJavaVersion)
    withSourcesJar()
}

repositories {
    maven("https://maven.notenoughupdates.org/releases/") {
        name = "NotEnoughUpdates"
    }
    maven("https://repo.hypixel.net/repository/Hypixel/") {
        name = "Hypixel"
    }
    maven("https://api.modrinth.com/maven") {
        name = "Modrinth"
        content {
            includeGroup("maven.modrinth")
        }
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$minecraftVersion")
    implementation("net.fabricmc:fabric-loader:$loaderVersion")
    implementation("net.fabricmc:fabric-language-kotlin:$kotlinLoaderVersion")

    implementation("net.fabricmc.fabric-api:fabric-api:$fabricVersion")
    compileOnly("net.hypixel:mod-api:$hypixelModApiVersion")
    include("maven.modrinth:1A2mKfBx:$hypixelModFabricVersion")
    shadowModImpl(project(":team-sync-protocol")) {
        isTransitive = false
    }
    shadowModImpl("org.notenoughupdates.moulconfig:modern-$minecraftVersion:$moulConfigVersion")
}

tasks.jar {
    archiveClassifier.set("dev")
}

tasks.shadowJar {
    enabled = false
}

val releaseJar = tasks.register<ShadowJar>("releaseJar") {
    dependsOn(tasks.jar)
    configurations = listOf(shadowModImpl)
    archiveClassifier.set("")
    from(tasks.jar.flatMap { it.archiveFile }.map { zipTree(it) })
    exclude("moulconfig.accesswidener")
    filesMatching("META-INF/*.kotlin_module") {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
    filesMatching("META-INF/services/**") {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
    relocate(
        "io.github.notenoughupdates.moulconfig",
        "sh.stefan.dragnevar.deps.moulconfig"
    )
    mergeServiceFiles()
    from("LICENSE.txt") {
        rename { "${it}_$archivesBaseName" }
    }
}

tasks.processResources {
    inputs.property("version", modVersion)
    inputs.property("minecraft_version", minecraftVersion)
    inputs.property("loader_version", loaderVersion)
    filteringCharset = "UTF-8"

    filesMatching("fabric.mod.json") {
        expand(
            "version" to modVersion,
            "minecraft_version" to minecraftVersion,
            "loader_version" to loaderVersion,
            "kotlin_loader_version" to kotlinLoaderVersion
        )
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(targetJavaVersion)
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.fromTarget(targetJavaVersion.toString()))
}

tasks.assemble {
    dependsOn(releaseJar)
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = archivesBaseName
            from(components["java"])
        }
    }
}
