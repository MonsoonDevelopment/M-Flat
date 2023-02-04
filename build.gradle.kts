import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("java")
    id("maven-publish")
    kotlin("jvm") version "1.7.21"
    
    application
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of("8"))
    }
}

group = "me.surge"
version = "1.0"

repositories {
    mavenCentral()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

application {
    mainClass.set("MainKt")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components.findByName("java"))
        }
    }
}