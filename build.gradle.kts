plugins {
    id("java")
}

group = "re.imc"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {

}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

tasks.compileJava {
    options.encoding = "UTF-8"
}