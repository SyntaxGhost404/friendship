package app.nearfolk

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import app.nearfolk.core.data.community.CommunitySimulator
import app.nearfolk.core.data.platform.AppVisibility
import app.nearfolk.notifications.Notifications
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class NearfolkApplication : Application() {

    @Inject lateinit var visibility: AppVisibility
    @Inject lateinit var community: CommunitySimulator

    override fun onCreate() {
        super.onCreate()
        Notifications.createChannels(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = visibility.setForeground(true)
            override fun onStop(owner: LifecycleOwner) = visibility.setForeground(false)
        })
        community.start()
    }
}
