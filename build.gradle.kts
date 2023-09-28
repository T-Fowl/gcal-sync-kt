import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.0"
}

group = "com.tfowl.gcal"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

    api("com.google.api-client:google-api-client:1.33.4")
    api("com.google.oauth-client:google-oauth-client-jetty:1.33.1")
    api("com.google.apis:google-api-services-calendar:v3-rev20220203-1.32.1")

    api("com.michael-bull.kotlin-result:kotlin-result:1.1.14")

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "20"
}