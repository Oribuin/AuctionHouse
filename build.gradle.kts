plugins {
    id("java-library")
    id("com.github.johnrengelman.shadow") version "7.1.0"
//    id("maven-publish")
}


group = "xyz.oribuin"
version = "1.1.0"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.isFork = true
    options.compilerArgs.add("-parameters")
}

repositories {
    mavenCentral()
    mavenLocal()

    maven("https://libraries.minecraft.net")
    maven("https://repo.rosewooddev.io/repository/public/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://jitpack.io/")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    api("dev.rosewood:rosegarden:1.2.5")
    api("org.jetbrains:annotations:24.0.0")
    api("dev.triumphteam:triumph-gui:3.1.7") { // Any higher versions stops the gui functioning
        exclude(group = "com.google.code.gson", module = "gson")
        exclude(group = "net.kyori", module = "*")
    }

    compileOnly("io.papermc.paper:paper-api:1.20.2-R0.1-SNAPSHOT")
    compileOnly("org.spigotmc:spigot-api:1.20.2-R0.1-SNAPSHOT")

    // Plugin Dependencies
    compileOnly("com.mojang:authlib:1.5.21")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
    compileOnly("com.arcaniax:HeadDatabase-API:1.3.1")
}


tasks {

    shadowJar {
        this.archiveClassifier.set("")

        this.relocate("org.jetbrains.annotations", "xyz.oribuin.auctionhouse.libs.annotations")
        this.relocate("org.intellij.lang", "xyz.oribuin.auctionhouse.libs.intellij")
        this.relocate("dev.rosewood.rosegarden", "xyz.oribuin.auctionhouse.libs.rosegarden")

        this.exclude("dev/rosewood/rosegarden/lib/hikaricp/**/*.class")
        this.exclude("dev/rosewood/rosegarden/lib/slf4j/**/*.class")
    }

    build {
        this.dependsOn("shadowJar")
    }

}