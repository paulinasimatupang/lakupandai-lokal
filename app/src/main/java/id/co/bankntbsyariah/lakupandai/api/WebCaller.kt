package id.co.bankntbsyariah.lakupandai.api

import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import org.json.JSONObject

interface WebCaller {
    val client: OkHttpClient

    fun fetchNasabahList(branchId: String, token: String): ResponseBody?
    fun fetchHistory(terminalId: String, token: String): ResponseBody?
    fun fetchHistoryDetail(terminalId: String, messageId: String, token: String): ResponseBody?
    fun changePassword(id: String, old_password: String, new_password: String, token: String): ResponseBody?
    fun forgotPassword(new_password: String, username: String, callback: (ResponseBody?) -> Unit)
    fun getPhoneByUsername(username: String): String?
    fun changePin(id: String, old_pin: String, new_pin: String, token: String): ResponseBody?
    fun blockAgen(id: String, token: String): ResponseBody?
    fun blockAgenLogin(username: String): ResponseBody?
    fun historyPengaduan(mid: String, token: String): ResponseBody?
    fun getParam2(serviceId: String, token: String): JSONObject?
    fun forgotPin(mid: String, token: String, callback: (Boolean, String?) -> Unit)
}