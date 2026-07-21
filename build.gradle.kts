import java.net.URI

plugins {
    `java-library`
    `maven-publish`
    id("com.gradleup.shadow") version "8.3.5"
}


group = "xyz.oribuin"
version = "1.1.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17

    disableAutoTargetJvm()
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
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
    api("dev.rosewood:rosegarden:1.5.3")
    api("dev.triumphteam:triumph-gui:3.1.11") {
        exclude(group = "com.google.code.gson", module = "gson")
        exclude(group = "net.kyori", module = "*")
    }

    compileOnly("io.papermc.paper:paper-api:1.21.3-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")

    // Plugin Dependencies
    compileOnly("org.jetbrains:annotations:24.0.0")
    compileOnly("com.mojang:authlib:1.5.21")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7")
    compileOnly("com.arcaniax:HeadDatabase-API:1.3.1")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            project.shadow.component(this)

            artifactId = "auctionhouse"
            pom {
                name.set("auctionhouse")
            }
        }

        repositories {
            if (project.hasProperty("mavenUser") && project.hasProperty("mavenPassword")) {
                maven {
                    credentials {
                        username = project.property("mavenUser") as String
                        password = project.property("mavenPassword") as String
                    }

                    val releasesRepoUrl = "https://repo.rosewooddev.io/repository/public-releases/"
                    val snapshotsRepoUrl = "https://repo.rosewooddev.io/repository/public-snapshots/"
                    url = URI(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
                }
            }

        }
    }
}

tasks {

    shadowJar {
        this.archiveClassifier.set("")

        this.relocate("org.intellij.lang", "${project.group}.auctionhouse.libs.intellij")
        this.relocate("dev.rosewood.rosegarden", "${project.group}.auctionhouse.libs.rosegarden")

        this.relocate("com.zaxxer", "${project.group}.auctionhouse.libs.hikari")
        this.relocate("org.slf4j", "${project.group}.auctionhouse.libs.slf4j")
    }

    build {
        this.dependsOn("shadowJar")
    }

}