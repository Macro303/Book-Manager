package github.buriedincode.bookshelf.models

data class CreatorInput(
    val credits: List<Credit> = ArrayList(),
    val image: String? = null,
    val name: String,
    val summary: String? = null,
) {
    data class Credit(
        val bookId: Long,
        val roleId: Long,
    )
}
