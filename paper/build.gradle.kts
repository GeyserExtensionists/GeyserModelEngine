plugins {
    id("java")
    id("com.gradleup.shadow") version "9.4.0"
    id("maven-publish")
}

group = "re.imc"
version = "1.0.9"

publishing {
    publications {
        create<MavenPublication>("shadow") {
            groupId = project.group.toString()
            artifactId = "GeyserModelEngine"
            version = project.version.toString()

            artifact(tasks.shadowJar.get().archiveFile) {
                builtBy(tasks.shadowJar)
            }
        }
    }

    repositories {
        mavenLocal()
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://central.sonatype.com/repository/maven-snapshots/")

    maven("https://mvn.lumine.io/repository/maven-public/")

    maven("https://repo.opencollab.dev/main/")

    maven("https://repo.codemc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-releases/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    implementation("dev.jorel:commandapi-paper-shade:11.2.0")

    compileOnly("com.ticxo.modelengine:ModelEngine:R4.1.0")
    compileOnly("io.github.toxicity188:bettermodel-api:2.2.0")
    compileOnly("io.github.toxicity188:bettermodel-bukkit-api:2.2.0")
    
    compileOnly(files("libs/geyserutils-spigot-1.0-SNAPSHOT.jar"))
    compileOnly("org.geysermc.floodgate:api:2.2.4-SNAPSHOT")

    implementation("com.github.retrooper:packetevents-spigot:2.13.0")
    implementation("org.bstats:bstats-bukkit:3.0.2")

    implementation("org.reflections:reflections:0.10.2")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.compileJava {
    options.encoding = "UTF-8"
}

tasks.shadowJar {
    archiveFileName.set("${rootProject.name}-${version}.jar")

    relocate("dev.jorel.commandapi", "re.imc.geysermodelengine.libs.commandapi")

    relocate("com.github.retrooper", "re.imc.geysermodelengine.libs.com.github.retrooper.packetevents")
    relocate("io.github.retrooper", "re.imc.geysermodelengine.libs.io.github.retrooper.packetevents")

    relocate("org.bstats", "re.imc.geysermodelengine.libs.bstats")

    relocate("org.reflections", "re.imc.geysermodelengine.libs.reflections")
}

tasks.build {
    dependsOn("shadowJar")
    finalizedBy("publishToMavenLocal")
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("paper-plugin.yml") {
        expand(props)
    }
}