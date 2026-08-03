import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm")
    id("com.gradleup.shadow")
}

version = providers.gradleProperty("mod_version").get()

val ktorVersion = "3.5.1"
val authlibVersion = "9.0.75"

repositories {
    mavenCentral()
    maven("https://libraries.minecraft.net")
}

dependencies {
    implementation(project(":team-sync-protocol"))
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-websockets:$ktorVersion")
    implementation("com.mojang:authlib:$authlibVersion")
    runtimeOnly("ch.qos.logback:logback-classic:1.5.18")
}

kotlin {
    jvmToolchain(21)
}

tasks.named<ShadowJar>("shadowJar") {
    archiveBaseName.set("team-sync-server")
    archiveVersion.set("")
    archiveClassifier.set("")
    filesMatching("META-INF/*.kotlin_module") {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
    filesMatching("META-INF/services/**") {
        duplicatesStrategy = DuplicatesStrategy.INCLUDE
    }
    mergeServiceFiles()
    manifest.attributes(
        "Main-Class" to "sh.stefan.dragnevar.teamsync.server.MainKt",
        "Implementation-Version" to project.version
    )
}

tasks.jar {
    enabled = false
}
