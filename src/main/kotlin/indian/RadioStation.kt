package indian

import kotlinx.serialization.Serializable

@Serializable
data class Station(
    val name: String,
    val tagLine: String,
    val description: String,
    val imageUrl: String,
    val streamingUrl: String,
    val languages: List<String>,
    val genre: List<String>,
    val firstAiredYear: String?,
    val bitRate: String?,
    val frequency: String?,
    val location: List<String>?,
    val rating: Rating?
)

@Serializable
data class Rating(
    val ratingOnFive: Float,
    val numberOfVotes: Int
)

@Serializable
data class Stations(
    val stations: Sequence<Station>
)

data class StationBuilder(
    var name: String = "",
    var tagLine: String = "",
    var description: String = "",
    var imageUrl: String = "",
    var streamingUrl: String = "",
    var languages: List<String> = listOf(),
    var genre: List<String> = listOf(),
    var firstAiredYear: String? = null,
    var bitRate: String? = null,
    var frequency: String? = null,
    var location: List<String> = listOf(),
    var rating: Rating? = null
) {
    fun build(): Station {
        return Station(
            name,
            tagLine,
            description,
            imageUrl,
            streamingUrl,
            languages,
            genre,
            firstAiredYear,
            bitRate,
            frequency,
            location,
            rating
        )
    }
}

fun Station() = StationBuilder()