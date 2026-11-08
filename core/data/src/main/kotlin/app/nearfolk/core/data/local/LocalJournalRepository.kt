package app.nearfolk.core.data.local

import app.nearfolk.core.common.TimeSource
import app.nearfolk.core.data.repository.JournalRepository
import app.nearfolk.core.database.dao.JournalDao
import app.nearfolk.core.database.entity.JournalEntity
import app.nearfolk.core.model.JournalEntry
import app.nearfolk.core.model.Mood
import app.nearfolk.core.model.SELF_ID
import app.nearfolk.core.model.Visibility
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class LocalJournalRepository @Inject constructor(
    private val journal: JournalDao,
    private val time: TimeSource,
) : JournalRepository {
    override fun myEntries(): Flow<List<JournalEntry>> = journal.observeFor(SELF_ID).map { rows -> rows.map { it.toModel() } }

    override fun publicEntries(memberId: String): Flow<List<JournalEntry>> =
        journal.observePublic(memberId).map { rows -> rows.map { it.toModel() } }

    override fun entry(id: String): Flow<JournalEntry?> = journal.observe(id).map { it?.toModel() }

    override suspend fun save(id: String?, title: String, body: String, mood: Mood, visibility: Visibility): String {
        val now = time.now().toEpochMilli()
        val existing = id?.let { journal.get(it) }?.takeIf { it.authorId == SELF_ID }
        val entity = JournalEntity(
            id = existing?.id ?: UUID.randomUUID().toString(),
            authorId = SELF_ID,
            title = title.trim(),
            body = body.trim(),
            mood = mood.name,
            visibility = visibility.name,
            createdAt = existing?.createdAt ?: now,
            updatedAt = now,
        )
        journal.upsert(entity)
        return entity.id
    }

    override suspend fun delete(id: String) {
        val row = journal.get(id) ?: return
        if (row.authorId == SELF_ID) journal.delete(id)
    }
}
