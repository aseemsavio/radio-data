package indian

import MAIN_DIRECTORY
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.nodes.Document
import ssl.SSLHelper

private class IndianRadioScraper(
    val mainDirectory: String,
    val baseUrl: String
) {
    suspend fun scrape() {
        var pageNumber = 1
        while (true) {
            val url = "$baseUrl/?page=$pageNumber"
            val doc = connect(url)
            if (doc.location() == "$baseUrl/" && pageNumber != 1) break

            // do stuff here

            println(doc.location())
            ++pageNumber
        }

    }

    private suspend fun connect(url: String): Document =
        withContext(Dispatchers.IO) { SSLHelper.getConnection(url).get() }
}

suspend fun scrapeIndianRadio() = IndianRadioScraper(MAIN_DIRECTORY, "https://onlineradiofm.in").scrape()