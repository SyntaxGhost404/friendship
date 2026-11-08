package app.nearfolk.core.database.di

import android.content.Context
import androidx.room.Room
import app.nearfolk.core.database.NearfolkDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): NearfolkDatabase =
        Room.databaseBuilder(context, NearfolkDatabase::class.java, "nearfolk.db").build()

    @Provides fun members(db: NearfolkDatabase) = db.memberDao()
    @Provides fun highlights(db: NearfolkDatabase) = db.highlightDao()
    @Provides fun posts(db: NearfolkDatabase) = db.postDao()
    @Provides fun comments(db: NearfolkDatabase) = db.commentDao()
    @Provides fun conversations(db: NearfolkDatabase) = db.conversationDao()
    @Provides fun messages(db: NearfolkDatabase) = db.messageDao()
    @Provides fun journal(db: NearfolkDatabase) = db.journalDao()
    @Provides fun safety(db: NearfolkDatabase) = db.safetyDao()
    @Provides fun activity(db: NearfolkDatabase) = db.activityDao()
}
