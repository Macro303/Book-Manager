package github.buriedincode.bookshelf.services

import org.apache.logging.log4j.kotlin.Logging

object OpenLibrary: Logging {
    private const val BASE_URL = "https://openlibrary.org"
    
    fun lookupBook(isbn: String) {
        val response = Utils.performJsonGet("$BASE_URL/isbn/$isbn.json", Edition.class)
            ?: throw InternalServerException(message = "Unable to find book with isbn: $isbn")
    }
    
    fun getBook(editionId: String) {
        
    }
}