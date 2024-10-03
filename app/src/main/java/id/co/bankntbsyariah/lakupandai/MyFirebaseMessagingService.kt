package id.co.bankntbsyariah.lakupandai

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.app.PendingIntent
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import id.co.bankntbsyariah.lakupandai.api.ApiService
import id.co.bankntbsyariah.lakupandai.local.Notification
import id.co.bankntbsyariah.lakupandai.local.NotificationDatabaseHelper
import id.co.bankntbsyariah.lakupandai.ui.MenuActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

const val channelId = "notification_channel"
const val channelName = "id.co.bankntbsyariah.lakupandai"

data class TokenPayload(val user_id: String, val fcm_token: String)

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private lateinit var notificationDbHelper: NotificationDatabaseHelper

    override fun onCreate() {
        super.onCreate()
        // Inisialisasi SQLite Helper
        notificationDbHelper = NotificationDatabaseHelper(this)
    }

    private fun saveAndGenerateNotification(title: String, message: String) {
        val timestamp = System.currentTimeMillis()

        // Simpan notifikasi ke SQLite
        saveNotificationToDatabase(title, message, timestamp)

        // Generate notifikasi untuk ditampilkan kepada pengguna
        generateNotification(title, message)
    }

    private fun saveNotificationToDatabase(title: String, message: String, timestamp: Long) {
        val notification = Notification(title = title, message = message, timestamp = timestamp)

        // Gunakan coroutine untuk menjalankan di thread background
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = notificationDbHelper.insertNotification(notification.title, notification.message, notification.timestamp)
                if (result) {
                    Log.d("NotificationSave", "Notifikasi berhasil disimpan: Title: $title, Message: $message")
                } else {
                    Log.e("NotificationSave", "Gagal menyimpan notifikasi.")
                }
            } catch (e: Exception) {
                Log.e("NotificationSave", "Error saat menyimpan notifikasi: ${e.message}")
            }
        }
    }


    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCM", "From: ${remoteMessage.from}")

        // Log notification message
        Log.d("FCM", "Notification Data: ${remoteMessage.data}")
        Log.d("FCM", "Notification Message: ${remoteMessage.notification?.body}")

        remoteMessage.notification?.let {
            saveAndGenerateNotification(it.title ?: "Notifikasi", it.body ?: "Anda memiliki pesan baru.")
        }

        if (remoteMessage.data.isNotEmpty()) {
            saveAndGenerateNotification(
                remoteMessage.data["title"] ?: "Notifikasi Data",
                remoteMessage.data["message"] ?: "Ada pesan penting!"
            )
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New FCM Token generated: $token")

        // Store token in global variable
        fcmToken = token

        // Retrieve authToken and userId from SharedPreferences
        val sharedPreferences = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
        val authToken = sharedPreferences.getString("auth_token", "") ?: ""
        val userId = sharedPreferences.getString("id", "") ?: ""  // Retrieve userId

        // Check if authToken and userId are available before sending to server
        if (authToken.isNotEmpty() && userId.isNotEmpty()) {
            sendFCMTokenToServer(authToken, token, userId)
        } else {
            Log.e("FCM", "Auth token or userId is empty. Cannot send FCM token to server.")
        }
    }

    private fun generateNotification(title: String, message: String) {
        val intent = Intent(this, MenuActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.mipmap.logo_aja_ntbs)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(1000, 1000, 1000, 1000))
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setContent(getRemoteView(title, message))

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(notificationChannel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    @SuppressLint("RemoteViewLayout")
    private fun getRemoteView(title: String, message: String): RemoteViews {
        val remoteView = RemoteViews("id.co.bankntbsyariah.lakupandai", R.layout.notification)
        remoteView.setTextViewText(R.id.title, title)
        remoteView.setTextViewText(R.id.message, message)
        remoteView.setImageViewResource(R.id.app_logo, R.mipmap.logo_aja_ntbs)
        return remoteView
    }
    companion object {
        var fcmToken: String? = null

        // Function to send FCM token and userId to server
        fun sendFCMTokenToServer(authToken: String, fcmToken: String, userId: String) {
            Log.d("FCM", "Sending FCM token to server: $fcmToken for user_id: $userId")

            val retrofit = Retrofit.Builder()
                .baseUrl("http://reportntbs.selada.id/api/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val apiService = retrofit.create(ApiService::class.java)

            // Include user_id when sending the FCM token
            val call: Call<ResponseBody> = apiService.updateFCMToken("Bearer $authToken", TokenPayload(userId, fcmToken))
            call.enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        Log.d("FCM", "FCM token successfully updated on the server")
                    } else {
                        Log.e("FCM", "Failed to update token on the server: ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.e("FCM", "Error updating FCM token on the server: ${t.message}")
                }
            })
        }
    }
}