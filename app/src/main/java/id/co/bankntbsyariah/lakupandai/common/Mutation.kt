package id.co.bankntbsyariah.lakupandai.common

data class Mutation(
    val date: String,
    val time: String,
    val description: String,
    val amount: String,
    val transactionType: String // Add transaction type to store CREDIT or DEBIT
)
