package com.estudoapp

import com.estudoapp.plugins.configureMonitoring
import com.estudoapp.plugins.configureRouting
import com.estudoapp.plugins.configureSecurity
import com.estudoapp.plugins.configureSerialization
import io.ktor.server.application.*


fun main(args: Array<String>): Unit = io.ktor.server.netty.EngineMain.main(args)

fun Application.module() {
    println("--- Iniciando configuração do Application.module ---")

    try {
        configureSecurity()
        configureRouting()
        configureSerialization()
        configureMonitoring()
        println("--- Configuração do module finalizada com sucesso! Servidor pronto. ---")

    } catch (e: Exception) {
        println("🔥🔥🔥 ERRO CRÍTICO DURANTE A INICIALIZAÇÃO 🔥🔥🔥")
        println("A aplicação falhou ao iniciar. Causa: ${e.message}")
        e.printStackTrace()
    }
}
