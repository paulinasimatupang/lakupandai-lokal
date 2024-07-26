package id.co.bankntbsyariah.lakupandai.api

import android.graphics.Bitmap
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.ResponseBody

interface ArrestCaller {
    val client: OkHttpClient

    fun fetchVersion(): ResponseBody?
    fun fetchRootMenuId(): ResponseBody?
    fun fetchScreen(id: String): String?
    fun fetchImage(id: String): Bitmap?
    fun requestPost()
}
