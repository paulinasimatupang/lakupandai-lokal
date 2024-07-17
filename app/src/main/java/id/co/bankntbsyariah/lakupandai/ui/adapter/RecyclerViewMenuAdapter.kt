package id.co.bankntbsyariah.lakupandai.ui.adapter

import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import id.co.bankntbsyariah.lakupandai.R
import id.co.bankntbsyariah.lakupandai.common.MenuItem
import id.co.bankntbsyariah.lakupandai.iface.ArrestCallerImpl
import id.co.bankntbsyariah.lakupandai.ui.MenuActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient

class RecyclerViewMenuAdapter(
    private val menuList: ArrayList<MenuItem>,
    private val context: Context
) : RecyclerView.Adapter<RecyclerViewMenuAdapter.MenuViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.recycler_view_menu_item,
            parent, false
        )

        return MenuViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        (context as MenuActivity).lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                val imgBitmap = ArrestCallerImpl(OkHttpClient()).fetchImage(menuList[position].image) ?:
                BitmapFactory.decodeResource(context.resources, R.mipmap.logo_aja_ntbs)
                withContext(Dispatchers.Main) {
                    holder.menuImage.setImageBitmap(imgBitmap)
                    holder.menuTitle.text = menuList[position].title
                    holder.menuSubtitle.text = menuList[position].subtitle
                    holder.menuDescription.text = menuList[position].description
                    holder.itemView.setOnClickListener {
                        (context as MenuActivity).onMenuItemClick(position)
                    }
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return menuList.size
    }

    class MenuViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val menuImage: ImageView = itemView.findViewById(R.id.header_image)
        val menuTitle: TextView = itemView.findViewById(R.id.title)
        val menuSubtitle: TextView = itemView.findViewById(R.id.subhead)
        val menuDescription: TextView = itemView.findViewById(R.id.body)
    }
}