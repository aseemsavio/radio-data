package indian

import MAIN_DIRECTORY
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jsoup.nodes.Document
import org.jsoup.select.Elements
import ssl.SSLHelper
import java.io.File
import java.lang.Exception

private class IndianRadioScraper(
    val mainDirectory: String,
    val baseUrl: String
) {
    suspend fun scrape() {
        val stations = scrape(pageNumber = 1)
        val format = Json { prettyPrint = true }
        val json = format.encodeToString(stations)

        val folder = File("Generated-JSON")
        if (!folder.exists()) folder.mkdir()

        val subFolder = File("Generated-JSON/Indian-Radios")
        if (!subFolder.exists()) subFolder.mkdir()

        val fileName = "Generated-JSON/Indian-Radios/indian-radios.json"
        File(fileName).createNewFile()
        File(fileName).writeText(json)
        println("Scraped successfully!")
    }

    private suspend fun IndianRadioScraper.scrape(
        pageNumber: Int
    ): List<Station> {
        val properRadios: MutableList<Station> = mutableListOf()
        var pageNumber1 = pageNumber
        while (true) {
            val url = "$baseUrl/?page=$pageNumber1"
            val doc = connect(url)
            if (doc.location() == "$baseUrl/" && pageNumber1 != 1) break
            val content: Elements = doc.getElementsByClass("container")[2].getElementsByClass("rowM")
            content.map {
                val items = it.select("a")
                items.map { element ->
                    val station = Station()
                    val productUrl = element.absUrl("href")
                    //println("$productUrl page: $pageNumber1")
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
                    val script = productContent.select("script").first().toString()
                    val audioUrl = script?.let { item ->
                        if (item.isNotEmpty() && item.contains("file:\"") && item.contains("\", autoplay")) getBetweenStrings(
                            item,
                            "file:\"",
                            "\", autoplay"
                        ) else ""
                    }
                    station.streamingUrl = audioUrl ?: ""
                    val ratingElement = productContent.select("div[itemProp = aggregateRating]")
                    station.rating = RatingBuilder(
                        ratingOnFive = ratingElement.select("meta[itemProp = ratingValue]").first()
                            ?.attr("content")
                            ?.toFloat(),
                        numberOfVotes = ratingElement.select("meta[itemProp = ratingCount]").first()
                            ?.attr("content")
                            ?.toInt()
                    )
                    val radioInfo = productContent.select("div.inforadio_new")
                    station.languages = extractList(radioInfo, "Language:", 0)
                    station.genre = extractList(radioInfo, "Genre:", 1)
                    station.description = radioInfo.first()?.getElementsByTag("p")?.get(2)?.text() ?: ""
                    val (firstAirDate, bitRate, frequency, location) = getExtraInformation(productContent)
                    try {
                        station.firstAiredYear = firstAirDate
                        station.bitRate = bitRate
                        station.frequency = frequency
                        station.location = location?.split(",")?.map { word -> word.trim() }!!
                    } catch (e: Exception) {
                        println("Error occurred: $e")
                    }

                    if (
                        station.name.isNotEmpty() &&
                        //it.tagLine.isNotEmpty() &&
                        station.description.isNotEmpty() &&
                        station.imageUrl.isNotEmpty() &&
                        station.streamingUrl.isNotEmpty() &&
                        station.languages.isNotEmpty() &&
                        station.genre.isNotEmpty()
                    ) {
                        properRadios.add(Station(
                            station.name,
                            tagLine = station.tagLine,
                            description = station.description,
                            imageUrl = station.imageUrl,
                            streamingUrl = station.streamingUrl,
                            languages = station.languages,
                            genre = station.genre,
                            firstAiredYear = station.firstAiredYear,
                            bitRate = station.bitRate,
                            frequency = station.frequency,
                            location = station.location,
                            rating = station.rating?.ratingOnFive?.let { it1 ->
                                station.rating?.numberOfVotes?.let { it2 ->
                                    Rating(
                                        ratingOnFive = it1,
                                        numberOfVotes = it2
                                    )
                                }
                            }
                        ))
                        println("Radios added: ${properRadios.size}")
                    }
                }
            }
            ++pageNumber1
        }
        return properRadios
    }

    private suspend fun IndianRadioScraper.getOnlyProperRadios(unoptimisedList: List<StationBuilder>) =
        sequence<Station> {
            unoptimisedList.forEach {
                if (
                    it.name.isNotEmpty() &&
                    //it.tagLine.isNotEmpty() &&
                    it.description.isNotEmpty() &&
                    it.imageUrl.isNotEmpty() &&
                    it.streamingUrl.isNotEmpty() &&
                    it.languages.isNotEmpty() &&
                    it.genre.isNotEmpty()
                ) {
                    yield(Station(
                        it.name,
                        tagLine = it.tagLine,
                        description = it.description,
                        imageUrl = it.imageUrl,
                        streamingUrl = it.streamingUrl,
                        languages = it.languages,
                        genre = it.genre,
                        firstAiredYear = it.firstAiredYear,
                        bitRate = it.bitRate,
                        frequency = it.frequency,
                        location = it.location,
                        rating = it.rating?.ratingOnFive?.let { it1 ->
                            it.rating?.numberOfVotes?.let { it2 ->
                                Rating(
                                    ratingOnFive = it1,
                                    numberOfVotes = it2
                                )
                            }
                        }
                    ))
                }
            }
        }

    private fun getExtraInformation(productContent: Elements): Extras {

        val ul = productContent?.select("div.inforadio_new > ul")
        val li = ul.select("li")

        var firstAirDate: String? = null
        var bitRate: String? = null
        var frequency: String? = null
        var location: String? = null

        li.indices.forEach { i ->
            val item = li[i]?.text()
            if (item?.contains("First air date") == true) {
                val date = item.split(":")[1].trim()
                firstAirDate = date.substring(date.length - 4)
            }
            if (item?.contains("Bitrate") == true) bitRate = item.split(":")[1].trim()
            if (item?.contains("Frequency") == true) frequency = item.split(":")[1].trim()
            if (item?.contains("Country") == true) location = item.split(":")[1].trim()
        }

        return Extras(
            firstAirDate, bitRate, frequency, location
        )
    }

    data class Extras(
        val firstAirDate: String?,
        val bitRate: String?,
        val frequency: String?,
        val location: String?
    )

    private fun extractList(
        radioInfo: Elements,
        selector: String,
        index: Int
    ): List<String> {
        val list = mutableListOf<String>()
        radioInfo.forEach { info ->
            val paragraph = info.getElementsByTag("p")[index]?.getElementsContainingText(selector)?.get(0)
            val aTags = paragraph?.getElementsByTag("a")
            aTags?.forEach { aTag ->
                var code = aTag.attr("href")
                val lastIndexOfSlash = code.lastIndexOf("/")
                code = if (code.isNotEmpty()) code.substring(lastIndexOfSlash + 1) else ""
                list += code
            }
        }
        return list
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

suspend fun scrapeIndianRadio() = getDataSequence()
suspend fun getDataSequence() {
    IndianRadioScraper(MAIN_DIRECTORY, "https://onlineradiofm.in").scrape()
}

fun Any.pp(indentSize: Int = 2) = " ".repeat(indentSize).let { indent ->
    toString()
        .replace(", ", ",\n$indent")
        .replace("(", "(\n$indent")
        .dropLast(1) + "\n)"
}
