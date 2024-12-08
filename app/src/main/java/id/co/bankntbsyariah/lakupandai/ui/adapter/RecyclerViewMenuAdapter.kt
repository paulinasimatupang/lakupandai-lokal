package id.co.bankntbsyariah.lakupandai.ui.adapter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.util.Log
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import id.co.bankntbsyariah.lakupandai.api.ArrestCaller
import id.co.bankntbsyariah.lakupandai.R
import id.co.bankntbsyariah.lakupandai.common.Constants
import id.co.bankntbsyariah.lakupandai.common.MenuItem
import id.co.bankntbsyariah.lakupandai.iface.ArrestCallerImpl
import id.co.bankntbsyariah.lakupandai.ui.MenuActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

class RecyclerViewMenuAdapter(
    private val menuList: MutableList<MenuItem>,
    private val context: Context,
    private val isHamburger: Boolean,
    private val isProfile: Boolean,
    private val isList: Boolean,
    private val isKomplain: Boolean,
    private val isKomplain2: Boolean,
    private val isProvider: Boolean,
    private val onItemClick: (Int) -> Unit
) : RecyclerView.Adapter<RecyclerViewMenuAdapter.MenuViewHolder>() {
    val TAG: String  // Define a tag for logging
        get() = "ImageFetcher"
    private val okHttpClient = OkHttpClient() // Reuse OkHttpClient instance
    private var formId = Constants.DEFAULT_ROOT_ID

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val itemView = layoutInflater.inflate(
            if (isHamburger) {
                R.layout.recycler_list_menu
            } else if(isProfile){
                R.layout.recycler_profile_list
            } else if(isList){
                R.layout.recycler_combo_box_provider
            } else if(isKomplain || isKomplain2){
                R.layout.recycler_menu_komplain
            } else if(isProvider){
                R.layout.recycler_view_menu_product
            }
            else {
                R.layout.recycler_view_menu_item
            },
            parent, false
        )
        return MenuViewHolder(itemView, isProfile, isKomplain, isKomplain2)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
//        Log.d(TAG, "Binding view holder for position $position")

        (context as MenuActivity).lifecycleScope.launch {

            val menuItem = menuList[position]
            val iconName = "${menuItem.image}.png"

            holder.menuImage?.let {
                Glide.with(context)
                    .load("http://16.78.84.90:8081/ARRest/static/$iconName")
//                    .load("https://lakupandai.bankntbsyariah.co.id/static/$iconName")
                    .apply(RequestOptions().placeholder(R.mipmap.logo_aja_ntbs))
                    .into(it)
            }

            holder.menuTitle.text = menuList[position].title

            // Tentukan apakah ini adalah item pertama di baris baru
            val isFirstInRow = position % 4 == 0 // Misalnya, 4 item per baris

            // Contoh untuk menghitung item dengan subtitle tertentu
            val specificItemCount = menuList.count { it.subtitle == "Transaksi Lainnya" }
//            Log.d(TAG, "Number of items with specific subtitle: $specificItemCount")


            // Selain dashboard
            if (specificItemCount != 0) {
                if (isFirstInRow) {
                    when (position) {
                        0 -> holder.textAboveRow?.text = "Lakupandai"
                        4 -> holder.textAboveRow?.text = "Biller"
                        else -> holder.textAboveRow?.visibility = View.GONE
                    }
                    holder.textAboveRow?.visibility = View.VISIBLE
                } else {
                    holder.textAboveRow?.visibility = View.GONE
                }
            } else {
                holder.textAboveRow?.visibility = View.GONE
            }

            if (isProfile) {
                holder.menuSubtitle?.visibility = View.GONE
                holder.menuDescription?.visibility = View.GONE
            } else {
                holder.menuSubtitle?.text = menuItem.subtitle
                holder.menuDescription?.text = menuItem.description
            }
            holder.itemView.setOnClickListener {
                Log.d(TAG, "Menu item with comp_icon: $iconName clicked")
                Log.d(TAG, "Menu item with title: ${menuItem.title} clicked")
                val itemType = when {
                    menuItem.originalLabel.startsWith("PLN", true) -> "menu"
                    menuItem.originalLabel.startsWith("PL", true) -> "pembelian"
                    menuItem.originalLabel.startsWith("PB", true) -> "pembayaran"
                    else -> "menu"
                }

                (context as MenuActivity).onMenuItemClick(position, itemType)
            }
        }
    }

    override fun getItemCount(): Int {
        return menuList.size
    }

    class MenuViewHolder(itemView: View, isProfile: Boolean, isKomplain: Boolean,  isKomplain2: Boolean) : RecyclerView.ViewHolder(itemView) {
        val menuTitle: TextView = itemView.findViewById(R.id.title)
        val textAboveRow: TextView? = itemView.findViewById(R.id.text_above_row)
        val menuImage: ImageView? = itemView.findViewById(R.id.header_image)
        val menuSubtitle: TextView? = if (!isProfile) itemView.findViewById(R.id.subhead) else null
        val menuDescription: TextView? = if (!isProfile) itemView.findViewById(R.id.body) else null

        init {
            if (isProfile){
                menuSubtitle?.visibility = View.GONE
                menuDescription?.visibility = View.GONE
            }
            if (isKomplain || isKomplain2){
                menuSubtitle?.visibility = View.GONE
                menuDescription?.visibility = View.GONE
                menuImage?.visibility = View.GONE
            }
        }
    }
}