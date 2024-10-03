package id.co.bankntbsyariah.lakupandai.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import id.co.bankntbsyariah.lakupandai.R
import id.co.bankntbsyariah.lakupandai.local.Notification

class NotificationListAdapter internal constructor(
    context: Context
) : RecyclerView.Adapter<NotificationListAdapter.NotificationViewHolder>() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private var notifications = emptyList<Notification>() // Cached copy of notifications

    inner class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val notificationTitle: TextView = itemView.findViewById(R.id.notification_title)
        val notificationMessage: TextView = itemView.findViewById(R.id.notification_message)
        val notificationTimestamp: TextView = itemView.findViewById(R.id.notification_timestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val itemView = inflater.inflate(R.layout.recyclerview_item, parent, false)
        return NotificationViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val current = notifications[position]
        holder.notificationTitle.text = current.title
        holder.notificationMessage.text = current.message
        holder.notificationTimestamp.text = android.text.format.DateFormat.format("dd-MM-yyyy hh:mm", current.timestamp)
    }

    internal fun setNotifications(notifications: List<Notification>) {
        this.notifications = notifications
        notifyDataSetChanged()
    }

    override fun getItemCount() = notifications.size
}