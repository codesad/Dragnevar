plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
}

version = providers.gradleProperty("mod_version").get()

repositories {
    mavenCentral()
}

dependencies {
    api("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
}

kotlin {
    jvmToolchain(21)
}
