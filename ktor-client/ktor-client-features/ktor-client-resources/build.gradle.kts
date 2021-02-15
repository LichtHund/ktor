description = "Server side Resources feature"

plugins {
    id("kotlinx-serialization")
}

val serialization_version: String by project.extra

kotlin.sourceSets{
    commonMain {
        dependencies {
            api(project(":ktor-resources-core"))
            api("org.jetbrains.kotlinx:kotlinx-serialization-core:$serialization_version")
        }
    }
}
