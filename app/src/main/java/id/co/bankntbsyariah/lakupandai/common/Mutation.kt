package id.co.bankntbsyariah.lakupandai.common

data class Mutation(
    val date: String,
    val time: String,
    val description: String,
    val amount: String,
    var transactionType: String,
    val archiveNumber: String
)
