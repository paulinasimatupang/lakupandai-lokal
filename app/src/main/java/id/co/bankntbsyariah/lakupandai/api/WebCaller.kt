package id.co.bankntbsyariah.lakupandai.api

import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import org.json.JSONObject

interface WebCaller {
    val client: OkHttpClient

    fun fetchNasabahList(branchId: String, token: String): ResponseBody?
}
