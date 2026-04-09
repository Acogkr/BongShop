plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
    alias(spigots.plugins.spigot)
}

group = "kr.acog"
version = "1.0.0"

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
    compileOnlySpigot(commons.typst.view.bukkitKotlin)
    compileOnly(commons.kotlinx.serialization.json)

}

kotlin {
    jvmToolchain(21)
}

spigot {
    authors = listOf("Acogkr")
    softDepend = listOf("Vault", "Coinsengine", "Nexo", "ItemsAdder", "PlaceholderAPI")
    apiVersion = "1.21"

    commands {
        register("상점") {
            description = "상점 명령어"
            usage = "/상점 도움말"
        }
    }
}
