package com.estudoapp.plugins

import com.estudoapp.domain.model.UserPrincipal
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import io.ktor.server.application.Application
import io.ktor.server.auth.authentication
import io.ktor.server.auth.bearer
import java.io.FileInputStream


fun Application.configureSecurity() {
    println("🔐 Configurando Firebase")
    
    if (FirebaseApp.getApps().isEmpty()) {
        try {

            // Diferentes estratégias de autenticação
            val credentials = when {
                // 1. Cloud Run com service account específico
                System.getenv("GOOGLE_APPLICATION_CREDENTIALS") != null -> {
                    println("📋 Usando GOOGLE_APPLICATION_CREDENTIALS: ${System.getenv("GOOGLE_APPLICATION_CREDENTIALS")}")
                    GoogleCredentials.getApplicationDefault()
                }
                
                // 2. Credenciais do metadata server (Cloud Run padrão)
                System.getenv("K_SERVICE") != null -> {
                    println("☁️ Usando credenciais do metadata server")
                    GoogleCredentials.getApplicationDefault()
                }
                
                // 3. Arquivo local (desenvolvimento)
                java.io.File("src/main/resources/service-account-key.json").exists() -> {
                    println("🏠 Usando arquivo local service-account-key.json")
                    GoogleCredentials.fromStream(
                        FileInputStream("src/main/resources/service-account-key.json")
                    )
                }
                
                // 4. Fallback para ADC
                else -> {
                    println("🔄 Fallback: tentando credenciais padrão")
                    GoogleCredentials.getApplicationDefault()
                }
            }
            
            val options = FirebaseOptions.builder()
                .setCredentials(credentials)
                .setDatabaseUrl("https://estudapp-71947-default-rtdb.firebaseio.com/")
                .setProjectId("estudapp-71947")
                .build()
                
            FirebaseApp.initializeApp(options)
            
            // Testar conexão
            val auth = FirebaseAuth.getInstance()
            println("✅ Firebase inicializado. Auth disponível: ${auth != null}")
            
        } catch (e: Exception) {
            println("❌ Falha crítica ao inicializar Firebase")
            println("Erro: ${e.message}")
            println("Stacktrace:")
            e.printStackTrace()
            
            // Informações de debug
            println("\n🔍 DEBUG INFO:")
            println("K_SERVICE: ${System.getenv("K_SERVICE")}")
            println("GOOGLE_APPLICATION_CREDENTIALS: ${System.getenv("GOOGLE_APPLICATION_CREDENTIALS")}")
            println("GAE_ENV: ${System.getenv("GAE_ENV")}")
            
            throw e
        }
    }

    authentication {
        bearer("firebase-auth") {
            authenticate { tokenCredential ->
                val token = tokenCredential.token
                try {
                    val decoded = FirebaseAuth.getInstance().verifyIdToken(token)
                    println("[AUTH] Token válido para uid: ${decoded.uid}")
                    UserPrincipal(uid = decoded.uid)
                } catch (e: Exception) {
                    println("[AUTH] Token inválido: ${e.message}")
                    null
                }
            }
        }
    }
}