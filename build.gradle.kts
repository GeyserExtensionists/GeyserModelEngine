plugins {
    id("java")
    id("io.github.goooler.shadow") version "8.1.7"
}

group = "re.imc"
version = "1.0.0"

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
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    implementation("dev.jorel:commandapi-bukkit-shade-mojang-mapped:10.1.2")

    compileOnly("com.ticxo.modelengine:ModelEngine:R4.0.9")

    compileOnly(files("libs/geyserutils-spigot-1.0-SNAPSHOT.jar"))
    compileOnly("org.geysermc.floodgate:api:2.2.4-SNAPSHOT")


    implementation("com.github.retrooper:packetevents-spigot:2.9.3")

    implementation("org.reflections:reflections:0.10.2")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.compileJava {
    options.encoding = "UTF-8"
}

tasks.shadowJar {
    relocate("dev.jorel.commandapi", "re.imc.geysermodelengine.libs.commandapi")

    relocate("com.github.retrooper", "re.imc.geysermodelengine.libs.com.github.retrooper.packetevents")
    relocate("io.github.retrooper", "re.imc.geysermodelengine.libs.io.github.retrooper.packetevents")

    relocate("org.reflections", "re.imc.geysermodelengine.libs.reflections")
}

tasks.jar {
    dependsOn(tasks.shadowJar)
}