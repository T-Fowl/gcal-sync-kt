import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "2.0.0"
}

group = "com.tfowl.gcal"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")

    api("com.google.api-client:google-api-client:2.6.0")
    api("com.google.oauth-client:google-oauth-client-jetty:1.36.0")
    api("com.google.apis:google-api-services-calendar:v3-rev20240705-2.0.0")

    api("com.michael-bull.kotlin-result:kotlin-result:2.0.0")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "20"
}