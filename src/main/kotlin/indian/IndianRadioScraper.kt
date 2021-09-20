package indian

import MAIN_DIRECTORY
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
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

            val content: Elements = doc.getElementsByClass("container")[2].getElementsByClass("rowM")

            content.forEach {
                val items = it.select("a")
                items.map { element ->
                    /*CoroutineScope(Dispatchers.Default).async {*/
                    val station = Station()
                    station.name = element.select("p").first()?.text().toString()
                    val productUrl = element.absUrl("href")
                    println(productUrl)
                    val productContent = connect(productUrl).getElementsByClass("content")
                    val titleElement = productContent.first()?.getElementById("radio-pagetitle")
                    station.name = titleElement?.text() ?: ""
                    val image = productContent
                        .first()
                        ?.getElementsByClass("logotip_new")
                        ?.select("img")
                        ?.first()
                        ?.absUrl("src")
                    station.imageUrl = image ?: ""

                    //val audioUrl = productContent.first()?.getElementById("player")
                    val script = productContent.select("script").first().toString()
                    //println("script: $script")
                    val audioUrl = script?.let { item ->
                        if (item.isNotEmpty() && item.contains("file:\"") && item.contains("\", autoplay")) getBetweenStrings(
                            item,
                            "file:\"",
                            "\", autoplay"
                        ) else ""
                    }
                    station.streamingUrl = audioUrl ?: ""
                    println("Data: $station")
                    // enter into each product's page

                }
                /*}.awaitAll()*/
            }

            //println("$content \n\n")
            //content.forEach { println(it) }

            // end here
            println(doc.location())
            ++pageNumber
        }

    }

    private suspend fun connect(url: String): Document =
        withContext(Dispatchers.IO) { SSLHelper.getConnection(url).get() }

    private fun getBetweenStrings(
        text: String,
        textFrom: String,
        textTo: String?
    ): String? {
        var result = ""

        // Cut the beginning of the text to not occasionally meet a
        // 'textTo' value in it:
        result = text.substring(
            text.indexOf(textFrom) + textFrom.length,
            text.length
        )

        // Cut the excessive ending of the text:
        result = result.substring(
            0,
            result.indexOf(textTo!!)
        )
        return result
    }
}

suspend fun scrapeIndianRadio() = IndianRadioScraper(MAIN_DIRECTORY, "https://onlineradiofm.in").scrape()