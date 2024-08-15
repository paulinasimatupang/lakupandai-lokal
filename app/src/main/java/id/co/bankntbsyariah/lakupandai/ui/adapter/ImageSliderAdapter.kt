package id.co.bankntbsyariah.lakupandai.ui.adapter

import android.content.Context
import android.graphics.drawable.Drawable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.RequestListener
import id.co.bankntbsyariah.lakupandai.R
import id.co.bankntbsyariah.lakupandai.common.BannerItem

class ImageSliderAdapter(
    private val imageList: List<BannerItem>,  // List of BannerItems
    private val context: Context,
    private val imageUrlBase: String  // Base URL for images
) : RecyclerView.Adapter<ImageSliderAdapter.SliderViewHolder>() {

    companion object {
        private const val TAG = "ImageSliderAdapter"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SliderViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.slider_item, parent, false)
        return SliderViewHolder(view)
    }

    override fun onBindViewHolder(holder: SliderViewHolder, position: Int) {
        val bannerItem = imageList[position]

        val imageUrl = "$imageUrlBase/${bannerItem.imageName}"  // Use the base URL and image name
        Log.d(TAG, "Loading image from URL: $imageUrl")  // Log the URL

        Glide.with(context)
            .load(imageUrl)
            .apply(
                RequestOptions()
                    .placeholder(R.mipmap.logo_aja_ntbs)  // Update with your placeholder image resource
                    .error(R.mipmap.ic_launcher_round)              // Update with your error image resource
            )
            .listener(object : RequestListener<Drawable> {
                override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Drawable>?, isFirstResource: Boolean): Boolean {
                    Log.e(TAG, "Image load failed: ${e?.message}", e)
                    return false  // Let Glide handle the error placeholder
                }

                override fun onResourceReady(resource: Drawable?, model: Any?, target: Target<Drawable>?, dataSource: DataSource?, isFirstResource: Boolean): Boolean {
                    Log.d(TAG, "Image successfully loaded")
                    return false  // Let Glide handle the resource
                }
            })
            .into(holder.imageView)
    }

    override fun getItemCount(): Int {
        return imageList.size
    }

    class SliderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.imageSlider)  // Ensure this matches the ID in slider_item.xml
    }
}
