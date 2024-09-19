package id.co.bankntbsyariah.lakupandai.api

import id.co.bankntbsyariah.lakupandai.Token
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import okhttp3.ResponseBody
import retrofit2.http.Header

data class Token(val token: String)

interface ApiService {
    @POST("api/update-token")
    fun updateFCMToken(@Header("Authorization") token: String, @Body fcmToken: Token): Call<ResponseBody>
}
