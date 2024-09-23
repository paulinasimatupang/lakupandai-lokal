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
    private val menuList: ArrayList<MenuItem>,
    private val context: Context,
    private val isHamburger: Boolean,
    private val isProfile: Boolean,
    private val isList: Boolean,
    private val isKomplain: Boolean,
    private val isKomplain2: Boolean

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
            }
            else {
                R.layout.recycler_view_menu_item
            },
            parent, false
        )
        return MenuViewHolder(itemView, isProfile, isKomplain, isKomplain2)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        Log.d(TAG, "Binding view holder for position $position")

        (context as MenuActivity).lifecycleScope.launch {
//            val iconName = "${menuList[position].image}.png"
//            Log.i(TAG, "Fetching Icon image with ID: $iconName")
//            val imgBitmap: Bitmap = try {
//                withContext(Dispatchers.IO) {
//                    Log.d(TAG, "Fetching image for component with comp_icon: $iconName")
//
//                    val fetchedBitmap = ArrestCallerImpl(okHttpClient).fetchImage(iconName)
//
//                    if (fetchedBitmap != null && isIconNameInCompIcon(iconName)) {
//                        Log.d(TAG, "Image for component with comp_icon: $iconName fetched successfully")
//                        fetchedBitmap
//                    } else {
//                        Log.d(TAG, "Image for component with comp_icon: $iconName not found on server")
//                        BitmapFactory.decodeResource(context.resources, R.mipmap.logo_aja_ntbs)
//                    }
//                }
//            } catch (e: Exception) {
//                Log.e(TAG, "Exception occurred while fetching image for component with comp_icon: $iconName", e)
//                BitmapFactory.decodeResource(context.resources, R.mipmap.logo_aja_ntbs)
//            }

            val menuItem = menuList[position]
            val iconName = "${menuItem.image}.png"

            holder.menuImage?.let {
                Glide.with(context)
                    .load("http://108.137.154.8:8081/ARRest/static/$iconName")
                    .apply(RequestOptions().placeholder(R.mipmap.logo_aja_ntbs))
                    .into(it)
            }

            holder.menuTitle.text = menuList[position].title

            if (isProfile) {
                holder.menuSubtitle?.visibility = View.GONE
                holder.menuDescription?.visibility = View.GONE
            } else {
                holder.menuSubtitle?.text = menuItem.subtitle
                holder.menuDescription?.text = menuItem.description
            }
            holder.itemView.setOnClickListener {
                Log.d(TAG, "Menu item with comp_icon: $iconName clicked")
                (context as MenuActivity).onMenuItemClick(position)
            }
        }
    }

    // Mock database query function
    fun isIconNameInCompIcon(iconName: String): Boolean {
        // Example mapping from component to icon
        val compIconMapping = mapOf(
            "Lupa pin" to "lupa_pin.png",
            "lupa sandi" to "lupa_sandi.png",
            "pdam" to "pdam.png",
            "ppob" to "lainnya.png",
            "transaksi" to "lainnya.png",
            "Gopay" to "Gopay.png",
            "OVO" to "OVO.png",
            "accounting" to "accounting.png",
            "atm" to "atm.png",
            "bank" to "bank.png",
            "bberhasil" to "berhasil.png",
            "bpjs" to "bpjs.png",
            "bpjs_form" to "bpjs_form.png",
            "catatan aktivitas" to "catatan_aktivitas.png",
            "cek saldo" to "cek_saldo.png",
            "cicilan" to "cicilan.png",
            "contact" to "contact.png",
            "faq" to "faq.png",
            "history" to "history.png",
            "internet" to "internet.png",
            "kembali" to "kembali.png",
            "komplain" to "komplain.png",
            "lainnya" to "lainnya.png",
            "logon" to "logon.png",
            "logout" to "logout.png",
            "mutasi" to "mutasi.png",
            "paket data" to "paket_data.png",
            "pdam form" to "pdam_form.png",
            "pemindahan bukuan" to "pemindahan_bukuan.png",
            "pengaturan printer" to "pengaturan_printer.png",
            "pengaturan profil" to "pengaturan_profil.png",
            "pulsa pascabayar form" to "pulsa_pascabayar_form.png",
            "pulsa pascabayar" to "pulsa_pascabayar.png",
            "pulsa regular" to "pulsa_regular.png",
            "setor tabungan" to "setor_tabungan.png",
            "shopeepay" to "shopeepay.png",
            "tarik tunai" to "tarik_tunai.png",
            "tiket" to "tiket.png",
            "transfer antar bank" to "transfer_antar_bank.png",
            "voucher game" to "voucher_game.png",
            "pln pascabayar" to "pln_pascabayar.png",
            "pln prabayar" to "pln_prabayar.png",
            "reversal" to "reversal.png",
            "create cif" to "create_cif.png",
            "create rekening" to "create_rekening.png",
        )
        // Check if the icon name exists in the mapping
        return compIconMapping.containsValue(iconName)
    }


    override fun getItemCount(): Int {
        return menuList.size
    }

    class MenuViewHolder(itemView: View, isProfile: Boolean, isKomplain: Boolean,  isKomplain2: Boolean) : RecyclerView.ViewHolder(itemView) {
        val menuTitle: TextView = itemView.findViewById(R.id.title)

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
