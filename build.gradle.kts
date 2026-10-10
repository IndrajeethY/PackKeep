plugins {
    id("fabric-loom") version "1.18.3"
}

val mc = stonecutter.current.version

version = "0.1.0+$mc"
group = "in.indrajeeth.packkeep"
base { archivesName.set("packkeep") }

repositories {
    maven("https://maven.terraformersmc.com/releases/")
    exclusiveContent {
        forRepository { maven("https://api.modrinth.com/maven") { name = "Modrinth" } }
        filter { includeGroup("maven.modrinth") }
    }
}

loom {
    splitEnvironmentSourceSets()
    mods {
        create("packkeep") {
            sourceSet(sourceSets["main"])
            sourceSet(sourceSets["client"])
        }
    }
}

data class Deps(val yarn: String, val loader: String, val fapi: String, val modmenu: String)

val d = when (mc) {
    "1.21" -> Deps("1.21+build.9", "0.19.5", "0.102.0+1.21", "11.0.5")
    "1.21.1" -> Deps("1.21.1+build.3", "0.19.5", "0.116.17+1.21.1", "11.0.5")
    "1.21.2" -> Deps("1.21.2+build.1", "0.19.5", "0.106.1+1.21.2", "12.0.1")
    "1.21.3" -> Deps("1.21.3+build.2", "0.19.5", "0.114.1+1.21.3", "12.0.1")
    "1.21.4" -> Deps("1.21.4+build.8", "0.19.5", "0.119.4+1.21.4", "13.0.4")
    "1.21.5" -> Deps("1.21.5+build.1", "0.19.5", "0.128.2+1.21.5", "14.0.2")
    "1.21.6" -> Deps("1.21.6+build.1", "0.19.5", "0.128.2+1.21.6", "15.0.2")
    "1.21.7" -> Deps("1.21.7+build.8", "0.19.5", "0.129.0+1.21.7", "15.0.2")
    "1.21.8" -> Deps("1.21.8+build.1", "0.19.5", "0.136.1+1.21.8", "15.0.2")
    "1.21.9" -> Deps("1.21.9+build.1", "0.19.5", "0.134.1+1.21.9", "16.0.1")
    "1.21.10" -> Deps("1.21.10+build.3", "0.19.5", "0.138.4+1.21.10", "16.0.1")
    "1.21.11" -> Deps("1.21.11+build.6", "0.19.5", "0.141.6+1.21.11", "17.0.1")
    else -> error("no dependency set configured for Minecraft $mc")
}

dependencies {
    minecraft("com.mojang:minecraft:$mc")
    mappings("net.fabricmc:yarn:${d.yarn}:v2")
    modImplementation("net.fabricmc:fabric-loader:${d.loader}")
    modImplementation("net.fabricmc.fabric-api:fabric-api:${d.fapi}")
    modImplementation("com.terraformersmc:modmenu:${d.modmenu}")
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
}

tasks.named<ProcessResources>("processResources") {
    inputs.property("version", project.version)
    filesMatching("fabric.mod.json") {
        expand("version" to project.version)
    }
}
