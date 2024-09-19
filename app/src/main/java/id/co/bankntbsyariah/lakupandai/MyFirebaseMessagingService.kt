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
data class Token(val token: String)

class MyFirebaseMessagingService : FirebaseMessagingService() {

    private val NOTIFICATION_PERMISSION_CODE = 1001

    // Fungsi ini akan dipanggil saat pesan FCM diterima
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCM", "From: ${remoteMessage.from}")

        // Tambahkan log untuk mencatat isi pesan
        Log.d("FCM", "Notification Data: ${remoteMessage.data}")
        Log.d("FCM", "Notification Message: ${remoteMessage.notification?.body}")

        // Handle notification message
        remoteMessage.notification?.let {
            requestNotificationPermission(it.title ?: "Notifikasi", it.body ?: "Anda memiliki pesan baru.")
        }

        // Handle data payload if present
        if (remoteMessage.data.isNotEmpty()) {
            Log.d("FCM", "Data Payload: ${remoteMessage.data}")
            requestNotificationPermission(
                remoteMessage.data["title"] ?: "Notifikasi Data",
                remoteMessage.data["message"] ?: "Ada pesan penting!"
            )
        }
    }

    // Function to handle new token generation and sending it to server
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New token: $token")

        // Tambahkan log untuk mencatat token yang baru dibuat
        Log.d("FCM", "New FCM Token generated: $token")

        // Call sendFCMTokenToServer with both authToken and fcmToken
        val authToken = retrieveAuthToken()  // Implement your logic to retrieve auth token
        if (authToken.isNotEmpty()) {
            sendFCMTokenToServer(authToken, token)  // Now passing both authToken and fcmToken
        } else {
            Log.e("FCM", "Auth token is empty. Cannot send FCM token to server.")
        }
    }

    // Permintaan izin notifikasi
    private fun requestNotificationPermission(title: String, message: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.d("FCM", "Izin notifikasi belum diberikan. Meminta izin...")

                // Menyimpan title dan message yang akan digunakan setelah izin diberikan
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
                return
            } else {
                Log.d("FCM", "Izin notifikasi telah diberikan.")
                generateNotification(title, message)
            }
        } else {
            // Tidak perlu izin untuk Android sebelum versi 13
            generateNotification(title, message)
        }
    }

    private fun generateNotification(title: String, message: String) {
        Log.d("FCM", "generateNotification called with title: $title, message: $message")
        Log.d("FCM", "Android version: ${Build.VERSION.SDK_INT}")

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

        Log.d("FCM", "Notifying notification manager...")
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

    private fun retrieveAuthToken(): String {
        // Implementasi pengambilan auth token
        val sharedPreferences = getSharedPreferences("MyAppPreferences", Context.MODE_PRIVATE)
        return sharedPreferences.getString("auth_token", "") ?: ""
    }

    companion object {
        fun sendFCMTokenToServer(authToken: String, fcmToken: String) {
            Log.d("FCM", "Updating FCM token on server: $fcmToken")

            // Ubah URL ini sesuai dengan server Anda
            val retrofit = Retrofit.Builder()
                .baseUrl("http://10.0.2.2:8000/")  // Gunakan IP emulator yang benar
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val apiService = retrofit.create(ApiService::class.java)

            val call: Call<ResponseBody> = apiService.updateFCMToken("Bearer $authToken", Token(fcmToken))
            call.enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        Log.d("FCM", "Token berhasil diperbarui di server")
                    } else {
                        Log.e("FCM", "Gagal memperbarui token: ${response.errorBody()?.string()}")
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Log.e("FCM", "Kesalahan saat memperbarui token di server: ${t.message}")
                }
            })
        }
    }
}
