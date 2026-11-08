package app.nearfolk.core.data.local

import app.nearfolk.core.common.TimeSource
import app.nearfolk.core.data.media.MediaFiles
import app.nearfolk.core.data.repository.AddHighlightResult
import app.nearfolk.core.data.repository.ProfileRepository
import app.nearfolk.core.database.dao.HighlightDao
import app.nearfolk.core.database.dao.MemberDao
import app.nearfolk.core.database.entity.HighlightEntity
import app.nearfolk.core.datastore.NearfolkPreferences
import app.nearfolk.core.model.IdentityUpdate
import app.nearfolk.core.model.MediaRef
import app.nearfolk.core.model.Member
import app.nearfolk.core.model.PlusLimits
import app.nearfolk.core.model.SELF_ID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class LocalProfileRepository @Inject constructor(
    private val members: MemberDao,
    private val highlights: HighlightDao,
    private val media: MediaFiles,
    private val prefs: NearfolkPreferences,
    private val time: TimeSource,
) : ProfileRepository {

    override val me: Flow<Member?> = combine(members.observe(SELF_ID), prefs.plusStatus) { row, plus ->
        row?.toModel()?.copy(isPlus = plus.active)
    }

    private suspend fun self() = members.get(SELF_ID) ?: error("No profile on this device")

    override suspend fun updateIdentity(update: IdentityUpdate) {
        members.upsert(
            self().copy(
                displayName = update.displayName.trim(),
                catchphrase = update.catchphrase.trim(),
                bio = update.bio.trim(),
                countryCode = update.countryCode,
                city = update.city?.trim()?.ifBlank { null },
                gender = update.gender.name,
                birthEpochDay = update.birthDate.toEpochDay(),
                streakStartEpochDay = update.streakStart.toEpochDay(),
            ),
        )
    }

    override suspend fun setAvatar(path: String?) {
        val current = self()
        (MediaRef.decode(current.avatar) as? MediaRef.Local)?.let { media.delete(it.path) }
        members.upsert(current.copy(avatar = path?.let { MediaRef.Local(it).encode() }))
    }

    override suspend fun setBanner(path: String?) {
        val current = self()
        (MediaRef.decode(current.banner) as? MediaRef.Local)?.let { media.delete(it.path) }
        members.upsert(current.copy(banner = path?.let { MediaRef.Local(it).encode() }))
    }

    override suspend fun addHighlight(path: String, caption: String?): AddHighlightResult {
        val limit = PlusLimits.highlights(prefs.plusStatus.first().active)
        if (highlights.count(SELF_ID) >= limit) {
            media.delete(path)
            return AddHighlightResult.LimitReached(limit)
        }
        highlights.insertAll(
            listOf(
                HighlightEntity(
                    id = UUID.randomUUID().toString(),
                    memberId = SELF_ID,
                    media = MediaRef.Local(path).encode(),
                    caption = caption?.trim()?.ifBlank { null },
                    createdAt = time.now().toEpochMilli(),
                ),
            ),
        )
        return AddHighlightResult.Added
    }

    override suspend fun removeHighlight(id: String) {
        val row = highlights.get(id) ?: return
        if (row.memberId != SELF_ID) return
        (MediaRef.decode(row.media) as? MediaRef.Local)?.let { media.delete(it.path) }
        highlights.delete(id)
    }

    override suspend fun setVoiceIntro(path: String, durationMillis: Long) {
        val current = self()
        media.delete(current.voicePath)
        members.upsert(current.copy(voicePath = path, voiceDurationMs = durationMillis, voiceRecordedAt = time.now().toEpochMilli()))
    }

    override suspend fun deleteVoiceIntro() {
        val current = self()
        media.delete(current.voicePath)
        members.upsert(current.copy(voicePath = null, voiceDurationMs = null, voiceRecordedAt = null))
    }
}
