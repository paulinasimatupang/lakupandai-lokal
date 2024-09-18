package id.co.bankntbsyariah.lakupandai.common

data class Component(
    var visible: Boolean,
    var type: Int,
    var id: String,
    var label: String,
    var action: String,
    var icon: String,
    var desc: String,
    var seq: Int,
    var opt: String,
    var values: List<Pair<String, String>> = emptyList(),
    var compValues: CompValues? = null
)

data class ComponentValue(
    val print: String?,
    var value: String?
)

data class CompValues(
    var compValue: List<ComponentValue>
)
