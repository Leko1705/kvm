plugins {
    kotlin("jvm") version "2.3.10"
}

group = "com.leko"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.ow2.asm:asm:9.7.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}