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
    val bitRate: String,
    val frequency: String,
    val location: String?
)

@Serializable
data class Stations(
    val stations: List<Station>
)