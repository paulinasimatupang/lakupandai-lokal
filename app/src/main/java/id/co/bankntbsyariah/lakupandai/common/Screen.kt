package id.co.bankntbsyariah.lakupandai.common

data class Screen(
    var type: Int,
    var title: String,
    var id: String,
    var ver: String,
    var action_url: String,
    var comp: ArrayList<Component>
)