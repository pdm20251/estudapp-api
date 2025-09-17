plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlin.plugin.serialization)
}

group = "com.estudoapp"
version = "0.0.1"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.firebase:firebase-admin:9.2.0")
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.server.netty)
    implementation(libs.logback.classic)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit)
}

jib {
    from {
        image = "eclipse-temurin:17-jre"
    }
    to {
        image = "gcr.io/estudapp-71947/estudapp-api"
        tags = setOf("latest")
    }
    container {
        ports = listOf("8080")
        environment = mapOf(
            "PORT" to "8080"
        )
        jvmFlags = listOf(
            "-server",
            "-Djava.awt.headless=true",
            "-XX:+UnlockExperimentalVMOptions",
            "-XX:+UseContainerSupport"
        )
    }
}
