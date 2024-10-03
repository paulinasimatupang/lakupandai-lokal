package id.co.bankntbsyariah.lakupandai.ui.theme

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import id.co.bankntbsyariah.lakupandai.R
import id.co.bankntbsyariah.lakupandai.local.NotificationDatabaseHelper
import id.co.bankntbsyariah.lakupandai.ui.adapter.NotificationListAdapter

class NotificationHistoryActivity : AppCompatActivity() {

    private lateinit var notificationDbHelper: NotificationDatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_history)

        notificationDbHelper = NotificationDatabaseHelper(this)

        val recyclerView = findViewById<RecyclerView>(R.id.recyclerview)
        val adapter = NotificationListAdapter(this)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Mengambil data dari SQLite dan menampilkannya di RecyclerView
        val notifications = notificationDbHelper.getAllNotifications()
        adapter.setNotifications(notifications)
    }
}
