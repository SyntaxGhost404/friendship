package app.nearfolk.core.data.di

import app.nearfolk.core.data.local.LocalChatRepository
import app.nearfolk.core.data.local.LocalJournalRepository
import app.nearfolk.core.data.local.LocalMemberRepository
import app.nearfolk.core.data.local.LocalPasscodeRepository
import app.nearfolk.core.data.local.LocalPlusRepository
import app.nearfolk.core.data.local.LocalPostRepository
import app.nearfolk.core.data.local.LocalPreferencesRepository
import app.nearfolk.core.data.local.LocalProfileRepository
import app.nearfolk.core.data.local.LocalSessionRepository
import app.nearfolk.core.data.repository.ChatRepository
import app.nearfolk.core.data.repository.JournalRepository
import app.nearfolk.core.data.repository.MemberRepository
import app.nearfolk.core.data.repository.PasscodeRepository
import app.nearfolk.core.data.repository.PlusRepository
import app.nearfolk.core.data.repository.PostRepository
import app.nearfolk.core.data.repository.PreferencesRepository
import app.nearfolk.core.data.repository.ProfileRepository
import app.nearfolk.core.data.repository.SessionRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataModule {
    @Binds abstract fun session(impl: LocalSessionRepository): SessionRepository
    @Binds abstract fun preferences(impl: LocalPreferencesRepository): PreferencesRepository
    @Binds abstract fun members(impl: LocalMemberRepository): MemberRepository
    @Binds abstract fun profile(impl: LocalProfileRepository): ProfileRepository
    @Binds abstract fun posts(impl: LocalPostRepository): PostRepository
    @Binds abstract fun chat(impl: LocalChatRepository): ChatRepository
    @Binds abstract fun journal(impl: LocalJournalRepository): JournalRepository
    @Binds abstract fun passcode(impl: LocalPasscodeRepository): PasscodeRepository
    @Binds abstract fun plus(impl: LocalPlusRepository): PlusRepository
}
