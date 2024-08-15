package id.co.bankntbsyariah.lakupandai.ui

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import id.co.bankntbsyariah.lakupandai.R

class PopupActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layoutId = intent.getIntExtra("LAYOUT_ID", R.layout.pop_up_berhasil) // Default to success
        setContentView(layoutId)

        val messageBody = intent.getStringExtra("MESSAGE_BODY") ?: "No message"
        val titleMessage = findViewById<TextView>(R.id.title_message)
        val bodyMessage = findViewById<TextView>(R.id.body_message)
        val popupButton = findViewById<Button>(R.id.popup_button)

        titleMessage.text = if (layoutId == R.layout.pop_up_berhasil) "Success" else "Gagal"
        bodyMessage.text = messageBody

        popupButton.setOnClickListener {
            finish()
        }
    }
}
