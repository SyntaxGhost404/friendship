package app.nearfolk.core.model

import java.time.Instant

enum class Mood { Calm, Hopeful, Grateful, Low, Restless, Tired }

enum class Visibility { Private, Public }

data class JournalEntry(
    val id: String,
    val authorId: String,
    val title: String,
    val body: String,
    val mood: Mood,
    val visibility: Visibility,
    val createdAt: Instant,
    val updatedAt: Instant,
)
