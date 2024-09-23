package id.co.bankntbsyariah.lakupandai.api

import id.co.bankntbsyariah.lakupandai.TokenPayload
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import okhttp3.ResponseBody
import retrofit2.http.Header

interface ApiService {
    @POST("fcm/update-token")
    fun updateFCMToken(
        @Header("Authorization") token: String,
        @Body fcmTokenPayload: TokenPayload // Mengirim user_id dan token dalam body sebagai JSON
    ): Call<ResponseBody>
}

