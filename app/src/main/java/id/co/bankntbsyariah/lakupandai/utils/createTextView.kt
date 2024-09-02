package id.co.bankntbsyariah.lakupandai.utils

import android.content.Context
import android.util.TypedValue
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat

fun createTextView(
    context: Context,
    text: String,
    textSize: Float,
    textStyle: Int,
    textColor: Int?,
    marginStart: Int,
    marginTop: Int
): TextView {
    return TextView(context).apply {
        this.text = text
        setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize)
        setTypeface(typeface, textStyle)
        textColor?.let { setTextColor(ContextCompat.getColor(context, it)) }
        val layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            setMargins(marginStart, marginTop, 0, 0)
        }
        this.layoutParams = layoutParams
    }
}
