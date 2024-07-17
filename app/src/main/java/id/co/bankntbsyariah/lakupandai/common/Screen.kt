package id.co.bankntbsyariah.lakupandai.common

data class Screen(
    var type: Int,
    var title: String,
    var id: String,
    var ver: String,
    var comp: ArrayList<Component>
)
