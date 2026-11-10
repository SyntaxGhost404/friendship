package app.nearfolk.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.nearfolk.MainActivity
import app.nearfolk.R
import app.nearfolk.core.data.platform.ChatNotifier
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject
import javax.inject.Singleton

object Notifications {
    const val CHANNEL_MESSAGES = "messages"
    const val CHANNEL_ACTIVITY = "activity"
    const val EXTRA_CONVERSATION = "conversation_id"
    private const val ACTIVITY_ID = 7

    fun createChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannels(
            listOf(
                NotificationChannel(CHANNEL_MESSAGES, context.getString(R.string.channel_messages), NotificationManager.IMPORTANCE_HIGH)
                    .apply { description = context.getString(R.string.channel_messages_body) },
                NotificationChannel(CHANNEL_ACTIVITY, context.getString(R.string.channel_activity), NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = context.getString(R.string.channel_activity_body) },
            ),
        )
    }

    internal fun activityId() = ACTIVITY_ID
}

@Singleton
class AndroidChatNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) : ChatNotifier {
    private val manager = NotificationManagerCompat.from(context)

    private fun allowed(): Boolean = manager.areNotificationsEnabled()

    /** Posts only when the runtime permission is held; the user can revoke it at any time. */
    private fun post(id: Int, notification: android.app.Notification) {
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        try {
            manager.notify(id, notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the call.
        }
    }

    override fun showMessage(conversationId: String, senderName: String, text: String, isRequest: Boolean) {
        if (!allowed()) return
        val intent = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(Notifications.EXTRA_CONVERSATION, conversationId)
        val pending = PendingIntent.getActivity(
            context,
            conversationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, Notifications.CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setContentTitle(senderName)
            .setContentText(text)
            .setSubText(if (isRequest) context.getString(R.string.notify_request) else null)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        post(conversationId.hashCode(), notification)
    }

    override fun showActivity(actorName: String, commentText: String?) {
        if (!allowed()) return
        val intent = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(context, Notifications.activityId(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val title = if (commentText == null) {
            context.getString(R.string.notify_like, actorName)
        } else {
            context.getString(R.string.notify_comment, actorName)
        }
        val notification = NotificationCompat.Builder(context, Notifications.CHANNEL_ACTIVITY)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setContentTitle(title)
            .setContentText(commentText)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        post(Notifications.activityId(), notification)
    }

    override fun showReply(actorName: String, replyText: String) {
        if (!allowed()) return
        val intent = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pending = PendingIntent.getActivity(context, Notifications.activityId(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(context, Notifications.CHANNEL_ACTIVITY)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.notification_accent))
            .setContentTitle(context.getString(R.string.notify_reply, actorName))
            .setContentText(replyText)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .build()
        post(Notifications.activityId(), notification)
    }

    override fun cancel(conversationId: String) {
        manager.cancel(conversationId.hashCode())
    }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class NotifierModule {
    @Binds
    abstract fun notifier(impl: AndroidChatNotifier): ChatNotifier
}
