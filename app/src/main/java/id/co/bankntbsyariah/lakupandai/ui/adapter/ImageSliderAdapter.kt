package id.co.bankntbsyariah.lakupandai.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import id.co.bankntbsyariah.lakupandai.R
import id.co.bankntbsyariah.lakupandai.common.Constants
import id.co.bankntbsyariah.lakupandai.common.BannerItem

class ImageSliderAdapter(
    private val imageList: List<BannerItem>,  // List of image names
    private val context: Context
) : RecyclerView.Adapter<ImageSliderAdapter.SliderViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SliderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.slider_item, parent, false)
        return SliderViewHolder(view)
    }

    override fun onBindViewHolder(holder: SliderViewHolder, position: Int) {
        val imageName = imageList[position]

        val imageUrl = "http://108.137.154.8:8081/ARRest/static/$imageName"
        Glide.with(context)
            .load(imageUrl)
            .apply(RequestOptions().placeholder(R.mipmap.logo_aja_ntbs))
            .into(holder.imageView)
    }

    override fun getItemCount(): Int {
        return imageList.size
    }

    // Check if the icon name exists in the mapping
    private fun isIconNameInCompIcon(imageName: String): Boolean {
        val compIconMapping = mapOf(
            "banner1" to "banner1.png",
            "banner2" to "banner2.png",
            "banner3" to "banner3.png",
        )
        return compIconMapping.containsValue(imageName)
    }

    class SliderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageSlider)
    }
}
