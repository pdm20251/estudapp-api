package com.estudoapp.infrastructure.persistence

import com.estudoapp.domain.Deck
import com.estudoapp.domain.repositories.DeckRepository
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class  FireBaseDeckRepository: DeckRepository {

    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference

    override suspend fun findByUserId(userId: String): List<Deck> {
        val decksRef = database.child("decks").child(userId)

        return suspendCoroutine { continuation ->
            decksRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val decks = snapshot.children.mapNotNull {
                        it.getValue(Deck::class.java)
                    }
                    continuation.resume(decks)
                }

                override fun onCancelled(error: DatabaseError) {
                    continuation.resumeWithException(error.toException())
                }
            })
        }
    }

    override suspend fun create(deck: Deck, userId: String): Deck {
        return withContext(Dispatchers.IO) {
            val decksRef = database.child("decks").child(userId)
            val newId = decksRef.push().key ?: UUID.randomUUID().toString()
            val deckWithId = deck.copy(id = newId)
            decksRef.child(newId).setValueAsync(deckWithId).get()
            deckWithId
        }
    }

    override suspend fun findById(deckId: String, userId: String): Deck? {
        val deckRef = database.child("decks").child(userId).child(deckId)

        return suspendCoroutine { continuation ->
            deckRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val deck = snapshot.getValue(Deck::class.java)
                    continuation.resume(deck)
                }
                override fun onCancelled(error: DatabaseError) {
                    continuation.resumeWithException(error.toException())
                }
            })
        }
    }
}