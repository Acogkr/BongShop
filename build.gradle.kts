plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
    alias(spigots.plugins.spigot)
    id("com.gradleup.shadow") version "8.3.6"
}

group = "kr.acog"
version = "2.0.0"

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://repo.extendedclip.com/releases/")
}

dependencies {
    compileOnly(spigots.paper.api)

    compileOnly(spigots.placeholderapi)
    compileOnly(spigots.vault.api) {
        isTransitive = false
    }
    compileOnly(fileTree("libs") { include("*.jar") })

    compileOnlySpigot(commons.typst.command.bukkit)
    compileOnlySpigot(commons.typst.command.kotlin)
    compileOnlySpigot(commons.typst.bukkitKotlinSerialization)
    implementation("io.typst:view-bukkit-kotlin:10.1.3")
    compileOnly(commons.kotlinx.serialization.json)

}

kotlin {
    jvmToolchain(21)
}

tasks.shadowJar {
    archiveClassifier.set("")
    dependencies {
        exclude(dependency("org.jetbrains.kotlin:.*:.*"))
        exclude(dependency("org.jetbrains.kotlinx:.*:.*"))
    }

    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")

    relocate("io.typst.view", "kr.acog.bongshop.shadow.io.typst.view")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

spigot {
    authors = listOf("Acogkr")
    softDepend = listOf("Vault", "Coinsengine", "PlaceholderAPI")
    apiVersion = "1.21"

    commands {
        register("상점") {
            permission = "bongshop.manager"
            description = "상점 명령어"
            usage = "/상점 도움말"
        }
    }
}
