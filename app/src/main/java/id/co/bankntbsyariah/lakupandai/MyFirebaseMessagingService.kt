package id.co.bankntbsyariah.lakupandai

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.app.PendingIntent
import android.util.Log
import android.widget.RemoteViews
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import id.co.bankntbsyariah.lakupandai.ui.MenuActivity
import id.co.bankntbsyariah.lakupandai.api.ApiService
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

    private val NOTIFICATION_PERMISSION_CODE = 1001

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

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCM", "From: ${remoteMessage.from}")

        // Log notification message
        Log.d("FCM", "Notification Data: ${remoteMessage.data}")
        Log.d("FCM", "Notification Message: ${remoteMessage.notification?.body}")

        remoteMessage.notification?.let {
            requestNotificationPermission(it.title ?: "Notifikasi", it.body ?: "Anda memiliki pesan baru.")
        }

        if (remoteMessage.data.isNotEmpty()) {
            Log.d("FCM", "Data Payload: ${remoteMessage.data}")
            requestNotificationPermission(
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

    private fun retrieveAuthToken(): String {
        // Retrieve auth token from SharedPreferences
        val sharedPreferences = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
        return sharedPreferences.getString("auth_token", "") ?: ""
    }

    private fun requestNotificationPermission(title: String, message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.d("FCM", "Notification permission not granted. Requesting permission...")

                val permissionIntent = Intent(applicationContext, MenuActivity::class.java)
                permissionIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                permissionIntent.putExtra("title", title)
                permissionIntent.putExtra("message", message)
                startActivity(applicationContext, permissionIntent, null)

                ActivityCompat.requestPermissions(
                    (applicationContext as? MenuActivity) ?: return,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
            } else {
                generateNotification(title, message)
            }
        } else {
            generateNotification(title, message)
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
}