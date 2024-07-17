package id.co.bankntbsyariah.lakupandai.iface

import id.co.bankntbsyariah.lakupandai.api.ArrestCaller
import okhttp3.OkHttpClient

class ArrestCallerImpl(override val client: OkHttpClient = OkHttpClient()) : ArrestCaller {

}