package id.co.bankntbsyariah.lakupandai.utils


import android.util.Log
import id.co.bankntbsyariah.lakupandai.common.CompValues
import id.co.bankntbsyariah.lakupandai.common.Component
import id.co.bankntbsyariah.lakupandai.common.ComponentValue
import id.co.bankntbsyariah.lakupandai.common.Screen
import org.json.JSONObject

class ScreenParser {
    companion object {
        fun parseJSON(data: JSONObject): Screen {
            val screen = data.optJSONObject("screen")
                ?: throw IllegalArgumentException("Missing 'screen' key in JSON data")

            val comp = screen.optJSONObject("comps")
                ?: throw IllegalArgumentException("Missing 'comps' key in 'screen' JSON data")

            val comps = comp.optJSONArray("comp")
                ?: throw IllegalArgumentException("Missing 'comp' key in 'comps' JSON data")

            val compArray = ArrayList<Component>()
            for (i in 0 until comps.length()) {
                val compJson = comps.optJSONObject(i)
                    ?: continue // Skip if JSON object is missing or invalid

                val valuesArray = ArrayList<Pair<String, String>>()
                if (compJson.has("comp_values")) {
                    val valuesJson = compJson.optJSONObject("comp_values")
                        ?: continue // Skip if JSON object is missing
                    val values = valuesJson.optJSONArray("comp_value")
                        ?: continue // Skip if JSON array is missing
                    for (j in 0 until values.length()) {
                        val valueJson = values.optJSONObject(j)
                            ?: continue // Skip if JSON object is missing
                        valuesArray.add(Pair(valueJson.optString("print", ""), valueJson.optString("value", "")))
                    }
                }

                val compValuesJson = compJson.optJSONObject("comp_values")
                val compValues = if (compValuesJson != null) {
                    val compValueArray = compValuesJson.optJSONArray("comp_value")
                    val compValueList = mutableListOf<ComponentValue>()

                    if (compValueArray != null) {
                        for (j in 0 until compValueArray.length()) {
                            val compValueJson = compValueArray.getJSONObject(j)
                            compValueList.add(
                                ComponentValue(
                                    compValueJson.optString("print"),
                                    compValueJson.optString("value")
                                )
                            )
                        }
                    }
                    CompValues(compValueList)
                } else {
                    null
                }

                compArray.add(
                    Component(
                        compJson.optBoolean("visible", false),
                        compJson.optInt("comp_type", -1),
                        compJson.optString("comp_id", ""),
                        compJson.optString("comp_lbl", ""),
                        compJson.optString("comp_act", ""),
                        compJson.optString("comp_icon", ""),
                        compJson.optString("comp_desc", ""),
                        compJson.optInt("seq", 0),
                        compJson.optString("comp_opt", ""),
                        valuesArray,
                        compValues
                    )
                )
            }

            // Extract and log action_url
            val actionUrl = screen.optString("action_url")
            Log.i("ScreenParser", "Action URL: $actionUrl")

            return Screen(
                screen.optInt("type", -1),
                screen.optString("title", ""),
                screen.optString("id", ""),
                screen.optString("ver", ""),
                compArray,
                actionUrl // Set actionUrl
            )
        }
    }
}
