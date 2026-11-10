package app.nearfolk.core.data.share

import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import app.nearfolk.core.data.R
import app.nearfolk.core.data.di.ApplicationScope
import app.nearfolk.core.data.repository.PostRepository
import app.nearfolk.core.model.Post
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Opens the system share sheet for a post. The share count only goes up once
 * the person picks an app: the sheet reports that choice to [PostShareReceiver].
 */
object PostSharing {

    internal const val EXTRA_POST_ID = "app.nearfolk.extra.POST_ID"

    fun share(context: Context, post: Post) {
        try {
            context.startActivity(chooser(context, post))
        } catch (_: ActivityNotFoundException) {
            // The system share sheet is always there; nothing to do if a device has none.
        }
    }

    /** The share sheet for [post], with a callback that reports the chosen app. */
    fun chooser(context: Context, post: Post): Intent {
        val appName = context.applicationInfo.loadLabel(context.packageManager).toString()
        val text = if (post.caption.isBlank()) {
            context.getString(R.string.data_share_photo, post.authorName, appName)
        } else {
            context.getString(R.string.data_share_text, post.authorName, appName, post.caption)
        }
        val send = Intent(Intent.ACTION_SEND)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, text)
        val chosen = Intent(context, PostShareReceiver::class.java)
            .putExtra(EXTRA_POST_ID, post.id)
            // Keeps one pending callback per post instead of reusing the last one.
            .setIdentifier(post.id)
        // Mutable so the system can add the chosen app to the callback.
        val callback = PendingIntent.getBroadcast(
            context,
            0,
            chosen,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        )
        return Intent.createChooser(send, context.getString(R.string.data_share_title), callback.intentSender)
    }
}

/** Counts a share once the share sheet says an app was picked. */
class PostShareReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val postId = intent.getStringExtra(PostSharing.EXTRA_POST_ID) ?: return
        val deps = EntryPointAccessors.fromApplication(context.applicationContext, ShareDependencies::class.java)
        val pending = goAsync()
        deps.scope().launch {
            try {
                deps.posts().recordShare(postId)
            } finally {
                pending.finish()
            }
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface ShareDependencies {
    fun posts(): PostRepository

    @ApplicationScope
    fun scope(): CoroutineScope
}
