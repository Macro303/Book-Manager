package github.buriedincode.openlibrary.schemas

import github.buriedincode.openlibrary.OpenLibrary
import github.buriedincode.openlibrary.SQLiteCache
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import java.nio.file.Paths

@TestInstance(Lifecycle.PER_CLASS)
class SearchResponseTest {
    private val session: OpenLibrary

    init {
        val cache = SQLiteCache(path = Paths.get("cache.sqlite"), expiry = null)
        session = OpenLibrary(cache = cache)
    }

    @Nested
    inner class Work {
        @Test
        fun `Test SearchWork with a valid search`() {
            val results = session.searchWork(params = mapOf("title" to "Dr. Stone, Vol. 1"))
            assertEquals(1, results.size)
            assertAll(
                { assertNull(results[0].alreadyReadCount) },
                { assertTrue(results[0].authorAlternativeName.isEmpty()) },
                { assertTrue(results[0].authorFacet.isEmpty()) },
                { assertEquals("OL2993106A", results[0].authorKey[0]) },
                { assertEquals("Riichiro Inagaki", results[0].authorName[0]) },
                { assertEquals("Cook, Caleb D., translator", results[0].contributor[0]) },
                { assertEquals("OL38630032M", results[0].coverEditionKey) },
                { assertEquals(12821031, results[0].cover) },
                { assertNull(results[0].currentlyReadingCount) },
                { assertEquals("741.5", results[0].ddc[0]) },
                { assertNull(results[0].ddcSort) },
                { assertEquals("no_ebook", results[0].ebookAccess) },
                { assertEquals(0, results[0].ebookCount) },
                { assertEquals(5, results[0].editionCount) },
                { assertEquals("OL38630032M", results[0].editionKey) },
                { assertEquals(2018, results[0].firstPublishYear) },
                { assertTrue(results[0].firstSentence.isEmpty()) },
                { assertEquals("Manga", results[0].format[0]) },
                { assertFalse(results[0].hasFulltext) },
                { assertTrue(results[0].ia.isEmpty()) },
                { assertTrue(results[0].iaBoxId.isEmpty()) },
                { assertTrue(results[0].iaCollection.isEmpty()) },
                { assertNull(results[0].iaCollectionString) },
                { assertTrue(results[0].iaLoadedId.isEmpty()) },
                { assertTrue(results[0].idAmazon.isEmpty()) },
                { assertTrue(results[0].idGoodreads.isEmpty()) },
                { assertTrue(results[0].idGoogle.isEmpty()) },
                { assertTrue(results[0].idLibrarything.isEmpty()) },
                { assertTrue(results[0].idOverdrive.isEmpty()) },
                { assertTrue(results[0].idProjectGutenberg.isEmpty()) },
                { assertTrue(results[0].idWikidata.isEmpty()) },
                { assertTrue(results[0].idDepositoLegal.isEmpty()) },
                { assertTrue(results[0].idIsfdb.isEmpty()) },
                { assertEquals("6076340487", results[0].isbn[0]) },
                { assertEquals("/works/OL37805541W", results[0].key) },
                { assertEquals("spa", results[0].language[0]) },
                { assertEquals(1715902450, results[0].lastModified) },
                { assertEquals("PN-6790.00000000.J34 D7313 2018", results[0].lcc[0]) },
                { assertEquals("2018299499", results[0].lccn[0]) },
                { assertNull(results[0].lccSort) },
                { assertNull(results[0].lendingEdition) },
                { assertNull(results[0].lendingIdentifier) },
                { assertEquals(196, results[0].numberOfPagesMedian) },
            )
        }
    }
}
