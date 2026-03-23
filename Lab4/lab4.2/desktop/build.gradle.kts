plugins {
    kotlin("jvm") version "1.9.22"
    application
}

group = "com.example"
version = "1.0"

repositories {
    mavenCentral()
    maven { url = uri("https://mvn.0110.be/releases") }
}

dependencies {
    implementation("be.tarsos.dsp:core:2.5")
    implementation("be.tarsos.dsp:jvm:2.5")
}

application {
    mainClass.set("com.example.pitchdetector.MainKt")
}

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.example.pitchdetector.MainKt"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}

kotlin {
    jvmToolchain(11)
}
