package id.co.bankntbsyariah.lakupandai.local

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log

class NotificationDatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "notification_db"
        private const val DATABASE_VERSION = 1
        private const val TABLE_NAME = "notifications"
        private const val COLUMN_ID = "id"
        private const val COLUMN_TITLE = "title"
        private const val COLUMN_MESSAGE = "message"
        private const val COLUMN_TIMESTAMP = "timestamp"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableStatement = ("CREATE TABLE $TABLE_NAME ("
                + "$COLUMN_ID INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "$COLUMN_TITLE TEXT, "
                + "$COLUMN_MESSAGE TEXT, "
                + "$COLUMN_TIMESTAMP LONG)")
        db.execSQL(createTableStatement)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertNotification(title: String, message: String, timestamp: Long): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues().apply {
            put(COLUMN_TITLE, title)
            put(COLUMN_MESSAGE, message)
            put(COLUMN_TIMESTAMP, timestamp)
        }

        val result = db.insert(TABLE_NAME, null, contentValues)
        db.close()
        return result != -1L
    }

    fun getAllNotifications(): List<Notification> {
        val notifications = mutableListOf<Notification>()
        val db = this.readableDatabase
        val cursor = db.query(TABLE_NAME, null, null, null, null, null, "$COLUMN_TIMESTAMP DESC")

        Log.d("Database", "Pengambilan data dari database dimulai")

        if (cursor.moveToFirst()) {
            do {
                // Pastikan kolom ada dengan menggunakan getColumnIndex() dan cek apakah index >= 0
                val titleIndex = cursor.getColumnIndex(COLUMN_TITLE)
                val messageIndex = cursor.getColumnIndex(COLUMN_MESSAGE)
                val timestampIndex = cursor.getColumnIndex(COLUMN_TIMESTAMP)

                if (titleIndex >= 0 && messageIndex >= 0 && timestampIndex >= 0) {
                    val title = cursor.getString(titleIndex)
                    val message = cursor.getString(messageIndex)
                    val timestamp = cursor.getLong(timestampIndex)
                    notifications.add(Notification(title, message, timestamp))
                    Log.d("Database", "Data ditemukan: Title: $title, Message: $message, Timestamp: $timestamp")
                } else {
                    Log.e("DatabaseError", "Kolom tidak ditemukan dalam tabel notifikasi")
                }
            } while (cursor.moveToNext())
        } else {
            Log.d("Database", "Tidak ada data notifikasi yang ditemukan")
        }
        cursor.close()
        db.close()

        Log.d("Database", "Pengambilan data dari database selesai, total data: ${notifications.size}")
        return notifications
    }

}

data class Notification(val title: String, val message: String, val timestamp: Long)