plugins {
    id("java")
    id("com.gradleup.shadow") version "9.2.2"
}

group = "me.zimzaza4"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()

    maven("https://repo.opencollab.dev/main/")

    maven("https://maven.tomalbrc.de")
}

dependencies {
    compileOnly("org.geysermc.geyser:api:2.9.0-SNAPSHOT")

    compileOnly(files("libs/geyserutils-geyser-1.0-SNAPSHOT.jar"))

    implementation("org.spongepowered:configurate-yaml:4.2.0-GeyserMC-SNAPSHOT")
    implementation("com.google.code.gson:gson:2.13.1")
    implementation("de.tomalbrc:blockbench-import-library:1.7.0+1.21.9")
}

tasks.shadowJar {
    archiveFileName.set("${rootProject.name}Extension-${version}.jar")

    relocate("org.spongepowered.configurate", "me.zimzaza4.geysermodelenginepackgenerator.libs.configurate")
}

tasks.build {
    dependsOn("shadowJar")
}
