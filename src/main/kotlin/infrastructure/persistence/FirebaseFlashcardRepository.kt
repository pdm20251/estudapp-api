package com.estudoapp.infrastructure.persistence

import com.estudoapp.domain.model.Alternativa
import com.estudoapp.domain.model.ClozeFlashcard
import com.estudoapp.domain.model.DigiteRespostaFlashcard
import com.estudoapp.domain.model.Flashcard
import com.estudoapp.domain.model.FrenteVersoFlashcard
import com.estudoapp.domain.model.MultiplaEscolhaFlashcard
import com.estudoapp.domain.repositories.FlashcardRepository
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@Suppress("UNCHECKED_CAST")
fun DataSnapshot.toFlashcard(): Flashcard? {
    val map = this.value as? Map<String, Any> ?: return null
    val type = map["type"] as? String

    return try {
        when (type) {
            "FRENTE_VERSO" -> FrenteVersoFlashcard(
                id = map["id"] as? String,
                deckId = map["deckId"] as? String,
                userId = map["userId"] as? String,
                frente = map["frente"] as? String,
                verso = map["verso"] as? String
            )
            "CLOZE" -> ClozeFlashcard(
                id = map["id"] as? String,
                deckId = map["deckId"] as? String,
                userId = map["userId"] as? String,
                textoComLacunas = map["textoComLacunas"] as? String,
                respostasCloze = map["respostasCloze"] as? Map<String, String>
            )
            "DIGITE_RESPOSTA" -> DigiteRespostaFlashcard(
                id = map["id"] as? String,
                deckId = map["deckId"] as? String,
                userId = map["userId"] as? String,
                pergunta = map["pergunta"] as? String,
                respostasValidas = map["respostasValidas"] as? List<String>
            )
            "MULTIPLA_ESCOLHA" -> {
                val alternativasMap = map["alternativas"] as? List<Map<String, Any>>
                val alternativas = alternativasMap?.map { altMap ->
                    Alternativa(
                        text = altMap["text"] as? String,
                        isCorrect = altMap["isCorrect"] as? Boolean
                    )
                }
                MultiplaEscolhaFlashcard(
                    id = map["id"] as? String,
                    deckId = map["deckId"] as? String,
                    userId = map["userId"] as? String,
                    pergunta = map["pergunta"] as? String,
                    alternativas = alternativas,
                    respostaCorreta = map["respostaCorreta"] as? String
                )
            }
            else -> null
        }
    } catch (e: Exception) {
        println("🔥🔥🔥 ERRO DE MAPEAMENTO para o flashcard com ID [${map["id"]}]: ${e.message}")
        null
    }
}

class FirebaseFlashcardRepository : FlashcardRepository {
    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference

    override suspend fun findById(deckId: String, flashcardId: String): Flashcard? {
        val flashcardRef = database.child("flashcards").child(deckId).child(flashcardId)
        return suspendCoroutine { continuation ->
            flashcardRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    continuation.resume(snapshot.toFlashcard())
                }
                override fun onCancelled(error: DatabaseError) {
                    continuation.resumeWithException(error.toException())
                }
            })
        }
    }

    override suspend fun findAllByDeckId(deckId: String, userId: String): List<Flashcard> {
        val ownerCheckRef = database.child("decks").child(userId).child(deckId)

        val isOwner = suspendCoroutine<Boolean> { continuation ->
            ownerCheckRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    continuation.resume(snapshot.exists())
                }
                override fun onCancelled(error: DatabaseError) {
                    continuation.resumeWithException(error.toException())
                }
            })
        }

        if (!isOwner) {
            println("[SECURITY] Tentativa de acesso negada ao deck '$deckId' pelo usuário '$userId'.")
            return emptyList()
        }

        val flashcardsRef = database.child("flashcards").child(deckId)
        return suspendCoroutine { continuation ->
            flashcardsRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        continuation.resume(emptyList())
                        return
                    }
                    val flashcards = snapshot.children.mapNotNull { it.toFlashcard() }
                    continuation.resume(flashcards)
                }
                override fun onCancelled(error: DatabaseError) {
                    continuation.resumeWithException(error.toException())
                }
            })
        }
    }

    override suspend fun create(deckId: String, userId: String, flashcard: Flashcard): Flashcard {
        return withContext(Dispatchers.IO) {
            // val flashcardsRef = database.child("flashcards").child(deckId)
            val newId = UUID.randomUUID().toString()

            // Criamos o objeto final para salvar no banco, garantindo a consistência dos IDs
            val finalFlashcard = when(flashcard) {
                is FrenteVersoFlashcard -> flashcard.copy(id = newId, deckId = deckId, userId = userId)
                is ClozeFlashcard -> flashcard.copy(id = newId, deckId = deckId, userId = userId)
                is DigiteRespostaFlashcard -> flashcard.copy(id = newId, deckId = deckId, userId = userId)
                is MultiplaEscolhaFlashcard -> flashcard.copy(id = newId, deckId = deckId, userId = userId)
            }

//            flashcardsRef.child(newId).setValueAsync(finalFlashcard).get()
            finalFlashcard
        }
    }

    override suspend fun saveAll(flashcards: List<Flashcard>) {
        if (flashcards.isEmpty()) {
            return
        }

        val childUpdates = mutableMapOf<String, Any?>()
        flashcards.forEach { flashcard ->
            val deckId = requireNotNull(flashcard.deckId) { "O deckId não pode ser nulo." }
            val flashcardId = requireNotNull(flashcard.id) { "O id não pode ser nulo." }

            val path = "/flashcards/$deckId/$flashcardId"
            childUpdates[path] = flashcard
        }

        return suspendCancellableCoroutine { continuation ->
            val completionListener = object : DatabaseReference.CompletionListener {
                override fun onComplete(error: DatabaseError?, ref: DatabaseReference) {
                    if (error == null) {
                        continuation.resume(Unit)
                    } else {
                        continuation.resumeWithException(error.toException())
                    }
                }
            }
            database.updateChildren(childUpdates, completionListener)
        }
    }
}