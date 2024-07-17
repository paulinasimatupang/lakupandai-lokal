package id.co.bankntbsyariah.lakupandai.utils

import id.co.bankntbsyariah.lakupandai.common.Component
import id.co.bankntbsyariah.lakupandai.common.Screen
import org.json.JSONObject

class ScreenParser {

    companion object {
        fun parseJSON(data: JSONObject): Screen {
            val screen = data.getJSONObject("screen")
            val comp = screen.getJSONObject("comps")
            val comps = comp.getJSONArray("comp")
            val compArray = ArrayList<Component>()
            for (i in 0 until comps.length()) {
                val compJson = comps.getJSONObject(i)
                val valuesArray = ArrayList<Pair<String, String>>()
                if (compJson.has("comp_values")) {
                    val valuesJson = compJson.getJSONObject("comp_values")
                    val values = valuesJson.getJSONArray("comp_value")
                    for (j in 0 until values.length()) {
                        val valueJson = values.getJSONObject(j)
                        valuesArray.add(Pair(valueJson.getString("print"), valueJson.getString("value")))
                    }
                }
                compArray.add(
                    Component(
                        compJson.getBoolean("visible"),
                        compJson.getInt("comp_type"),
                        compJson.getString("comp_id"),
                        compJson.getString("comp_lbl"),
                        compJson.optString("comp_act", ""),
                        compJson.optString("comp_icon", ""),
                        compJson.optString("comp_desc", ""),
                        compJson.getInt("seq"),
                        valuesArray
                    )
                )
            }
            return Screen(
                screen.getInt("type"),
                screen.getString("title"),
                screen.getString("id"),
                screen.getString("ver"),
                compArray
            )
        }
    }
}