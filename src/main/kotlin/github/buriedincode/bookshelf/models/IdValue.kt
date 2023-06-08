package github.buriedincode.bookshelf.models

data class IdValue(
    val id: Long
) {
    override fun toString(): String {
        return "IdValue(id=$id)"
    }
}
