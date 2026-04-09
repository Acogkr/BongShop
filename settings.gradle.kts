rootProject.name = "BongShop"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }

    versionCatalogs {
        create("spigots") {
            from("io.typst:spigot-catalog:1.0.0")
        }

        create("commons") {
            from("io.typst:common-catalog:1.1.0")
        }
    }
}
