package com.estudoapp.plugins

import com.estudoapp.domain.UserPrincipal
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import io.ktor.http.HttpStatusCode
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.application.Application
import io.ktor.server.auth.authentication
import io.ktor.server.auth.bearer
import java.io.FileInputStream


fun Application.configureSecurity() {
    if (FirebaseApp.getApps().isEmpty()) {
        val serviceAccount = FileInputStream("src/main/resources/service-account-key.json")
        val databaseUrl = "https://estudapp-71947-default-rtdb.firebaseio.com/"
        val options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(serviceAccount))
            .setDatabaseUrl(databaseUrl)
            .build()
        FirebaseApp.initializeApp(options)
    }

    authentication {
        bearer("firebase-auth") {  // 👉 troquei para bearer em vez de jwt
            authenticate { tokenCredential ->
                val token = tokenCredential.token  // aqui vem o Bearer cru
                try {
                    val decoded = FirebaseAuth.getInstance().verifyIdToken(token)

                    println("[DEBUG] Token válido para uid: ${decoded.uid}")

                    // Se o token é válido, devolvemos o principal
                    UserPrincipal(uid = decoded.uid)
                } catch (e: Exception) {
                    println("❌ Erro ao verificar token Firebase: ${e.message}")
                    null
                }
            }
        }
    }
}