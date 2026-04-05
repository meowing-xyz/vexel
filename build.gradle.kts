import dev.deftu.gradle.utils.version.MinecraftVersions

plugins {
    java
    kotlin("jvm")
    id("dev.deftu.gradle.multiversion")
    id("dev.deftu.gradle.tools")
    id("dev.deftu.gradle.tools.resources")
    id("dev.deftu.gradle.tools.bloom")
    id("dev.deftu.gradle.tools.shadow")
    id("dev.deftu.gradle.tools.minecraft.loom")
    id("dev.deftu.gradle.tools.minecraft.releases")
    id("dev.deftu.gradle.tools.publishing.maven")
}

//apply(rootProject.file("secrets.gradle.kts"))

toolkitMultiversion.moveBuildsToRootProject.set(true)
toolkitLoomHelper.useMixinRefMap(modData.id)
repositories.maven("https://maven.teamresourceful.com/repository/maven-public/")
repositories.mavenLocal() // TODO: remove once knit-1.21.11-fabric is published to remote maven

dependencies {
    implementation(kotlin("stdlib-jdk8"))

    if (mcData.isFabric) {
        modImplementation("net.fabricmc.fabric-api:fabric-api:${mcData.dependencies.fabric.fabricApiVersion}")
        modImplementation("net.fabricmc:fabric-language-kotlin:${mcData.dependencies.fabric.fabricLanguageKotlinVersion}")
    }

    modApi(include("xyz.meowing:knit-$mcData:118")!!)

    api(shade("org.lwjgl:lwjgl-nanovg:3.3.3")!!)
    listOf("windows", "linux", "macos", "macos-arm64").forEach { v ->
        api(shade("org.lwjgl:lwjgl-nanovg:3.3.3:natives-$v")!!)
    }

    when (mcData.version) {
        MinecraftVersions.VERSION_1_21_11 -> include(modApi("earth.terrarium.olympus:olympus-fabric-1.21.11:1.7.2")!!)
        MinecraftVersions.VERSION_1_21_10 -> include(modApi("earth.terrarium.olympus:olympus-fabric-1.21.9:1.6.2")!!)
        MinecraftVersions.VERSION_1_21_8 -> include(modApi("earth.terrarium.olympus:olympus-fabric-1.21.7:1.5.2")!!)
        MinecraftVersions.VERSION_1_21_5 -> include(modApi("earth.terrarium.olympus:olympus-fabric-1.21.5:1.3.6")!!)
        else -> {}
    }
}

toolkitMavenPublishing {
    artifactName.set("vexel")
    setupRepositories.set(false)
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    compilerOptions {
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
    }
}

afterEvaluate {
    publishing {
        publications {
            named<MavenPublication>("mavenJava") {
                pom {
                    name.set("Vexel")
                    description.set("A simple declarative rendering library built with lwjgl's NanoVG Renderer")
                    url.set("https://github.com/meowing-xyz/vexel")
                    licenses {
                        license {
                            name.set("GNU General Public License v3.0")
                            url.set("https://www.gnu.org/licenses/gpl-3.0.en.html")
                        }
                    }
                    developers {
                        developer {
                            id.set("aurielyn")
                            name.set("Aurielyn")
                        }
                        developer {
                            id.set("mrfast")
                            name.set("MrFast")
                        }
                    }
                    scm {
                        url.set("https://github.com/meowing-xyz/vexel")
                    }
                }
            }
        }
        repositories {
            maven {
                name = "Bundle"
                url = uri(layout.buildDirectory.dir("central-bundle"))
            }
        }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications)
}

val createBundle = tasks.register<Zip>("createBundle") {
    from(layout.buildDirectory.dir("central-bundle"))
    archiveFileName.set("vexel:$version")
    destinationDirectory.set(layout.buildDirectory)
    dependsOn("publishMavenJavaPublicationToBundleRepository")
}

tasks.register<Exec>("publishToSonatype") {
    group = "publishing"
    dependsOn(createBundle)
    commandLine(
        "curl", "-X", "POST",
        "-u", "${findProperty("sonatype.username")}:${findProperty("sonatype.password")}",
        "-F", "bundle=@${layout.buildDirectory.file("vexel:$version").get().asFile.absolutePath}",
        "https://central.sonatype.com/api/v1/publisher/upload?publishingType=AUTOMATIC"
    )
}