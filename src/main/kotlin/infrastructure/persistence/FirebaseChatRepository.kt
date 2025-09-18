package com.estudoapp.infrastructure.persistence

import com.estudoapp.domain.model.ChatMessage
import com.estudoapp.domain.repositories.ChatRepository
import com.google.firebase.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class FirebaseChatRepository : ChatRepository {

    private val database: DatabaseReference = FirebaseDatabase.getInstance().reference

    override suspend fun getLatestMessages(userId: String, limit: Int): List<ChatMessage> {
        val chatRef = database.child("chats").child(userId)
        // Usamos uma query do Firebase para ordenar por chave (que é cronológica) e pegar as últimas N
        val query = chatRef.orderByKey().limitToLast(limit)

        return suspendCoroutine { continuation ->
            query.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val messages = snapshot.children.mapNotNull {
                        it.getValue(ChatMessage::class.java)
                    }
                    continuation.resume(messages)
                }
                override fun onCancelled(error: DatabaseError) {
                    continuation.resumeWithException(error.toException())
                }
            })
        }
    }

    override suspend fun addMessage(userId: String, message: ChatMessage) {
        val chatRef = database.child("chats").child(userId)
        val newId = chatRef.push().key ?: ""
        val messageWithId = message.copy(id = newId)


        withContext(Dispatchers.IO) {
            chatRef.child(newId).setValueAsync(messageWithId).get()
        }
    }
}