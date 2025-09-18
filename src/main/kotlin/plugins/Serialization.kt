package com.estudoapp.plugins

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(Json {
            // CORREÇÃO: Alterado de "_type" para "type" para corresponder
            // ao JSON retornado pela IA e à sua sealed class `Flashcard`.
            classDiscriminator = "type"
            ignoreUnknownKeys = true // Mantido por segurança
        })
    }
}
