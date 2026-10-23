package com.markooverhaul.sunset.core.data.fake

import com.markooverhaul.sunset.core.model.Category
import com.markooverhaul.sunset.core.model.CategoryType
import com.markooverhaul.sunset.core.model.Episode
import com.markooverhaul.sunset.core.model.LiveChannel
import com.markooverhaul.sunset.core.model.Movie
import com.markooverhaul.sunset.core.model.Season
import com.markooverhaul.sunset.core.model.SeriesDetail
import com.markooverhaul.sunset.core.model.SeriesSummary
import java.util.Calendar

/**
 * Local, entirely fictional sample catalogue.
 *
 * This is NOT provider data. Titles, people, plots and channel names were invented for the demo so
 * no production catalogue, artwork or metadata is scraped or reconstructed. All artwork URLs are
 * null, which makes every screen render fully offline via gradient/monogram placeholders.
 */
object FakeCatalog {

    private const val MINUTE = 60_000L
    private const val HOUR = 60 * MINUTE
    private const val DAY = 24 * HOUR

    private fun daysAgo(days: Int): Long = System.currentTimeMillis() - days * DAY

    val movieCategories: List<Category> = listOf(
        "Action", "Drama", "Comedy", "Family", "Documentary", "Thriller",
    ).mapIndexed { index, name ->
        Category(id = "movie-${name.lowercase()}", type = CategoryType.MOVIE, name = name, sortOrder = index)
    }

    val seriesCategories: List<Category> = listOf(
        "Drama", "Comedy", "Crime", "Documentary",
    ).mapIndexed { index, name ->
        Category(id = "series-${name.lowercase()}", type = CategoryType.SERIES, name = name, sortOrder = index)
    }

    val liveCategories: List<Category> = listOf(
        "News", "Sports", "Movies", "Entertainment", "Kids",
    ).mapIndexed { index, name ->
        Category(id = "live-${name.lowercase()}", type = CategoryType.LIVE, name = name, sortOrder = index)
    }

    val movies: List<Movie> = listOf(
        Movie(
            id = "mv-amber-horizon",
            title = "Amber Horizon",
            year = 2024,
            plot = "A retired lighthouse keeper and a runaway astronomer cross a fading coastline in search of a signal only they can hear. A quiet story about second chances and the light we leave behind.",
            rating = 8.1,
            durationMinutes = 124,
            genres = listOf("Drama", "Adventure"),
            cast = listOf("Mara Velez", "Tom Okafor", "Ines Duarte"),
            director = "Lena Marchetti",
            certification = "PG-13",
            addedAtUtcMs = daysAgo(2),
        ),
        Movie(
            id = "mv-night-signal",
            title = "Night Signal",
            year = 2023,
            plot = "A late-night radio host starts receiving calls from a town that disappeared twenty years ago. The answer is closer to the studio than she wants to admit.",
            rating = 7.4,
            durationMinutes = 108,
            genres = listOf("Thriller", "Drama"),
            cast = listOf("Priya Raman", "Elias Nord", "June Park"),
            director = "Owen Battersea",
            certification = "R",
            addedAtUtcMs = daysAgo(5),
        ),
        Movie(
            id = "mv-last-lighthouse",
            title = "The Last Lighthouse",
            year = 2025,
            plot = "When the final keeper of a remote lighthouse is told to shut it down, he spends one last winter documenting the ships that still rely on its beam.",
            rating = 7.8,
            durationMinutes = 96,
            genres = listOf("Drama"),
            cast = listOf("Samir Haddad", "Nora Lind", "Ben Achebe"),
            director = "Clara Voss",
            certification = "PG",
            addedAtUtcMs = daysAgo(1),
        ),
        Movie(
            id = "mv-copper-sky",
            title = "Copper Sky",
            year = 2022,
            plot = "A cargo pilot agrees to one last run across a desert that has swallowed three planes before hers. The storm is not the only thing following her.",
            rating = 7.0,
            durationMinutes = 132,
            genres = listOf("Action", "Adventure"),
            cast = listOf("Rhea Solano", "Marcus Bell", "Yuki Tan"),
            director = "Diego Ferrer",
            certification = "PG-13",
            addedAtUtcMs = daysAgo(12),
        ),
        Movie(
            id = "mv-paper-boats",
            title = "Paper Boats",
            year = 2024,
            plot = "Two siblings turn a rainy summer into an elaborate river expedition, learning more about their grandparents than either expected.",
            rating = 6.9,
            durationMinutes = 88,
            genres = listOf("Family", "Comedy"),
            cast = listOf("Mia Chen", "Leo Grant", "Ada Mensah"),
            director = "Sofia Park",
            certification = "G",
            addedAtUtcMs = daysAgo(3),
        ),
        Movie(
            id = "mv-static-bloom",
            title = "Static Bloom",
            year = 2021,
            plot = "A patient portrait of the engineers who keep a century-old broadcast tower alive through storms, budget cuts and one very stubborn transmitter.",
            rating = 7.6,
            durationMinutes = 74,
            genres = listOf("Documentary"),
            cast = listOf("The Meridian Tower crew"),
            director = "Hugo Lindqvist",
            certification = "PG",
            addedAtUtcMs = daysAgo(20),
        ),
        Movie(
            id = "mv-midnight-diner",
            title = "Midnight Diner",
            year = 2020,
            plot = "A night-shift cook serves a different story at every table, and slowly admits that his own is the one he has been avoiding.",
            rating = 7.2,
            durationMinutes = 101,
            genres = listOf("Comedy", "Drama"),
            cast = listOf("Kenji Mori", "Lucia Reyes", "Omar Faye"),
            director = "Ana Sokolova",
            certification = "PG-13",
            addedAtUtcMs = daysAgo(30),
        ),
        Movie(
            id = "mv-quiet-wild",
            title = "The Quiet Wild",
            year = 2023,
            plot = "A year in the life of a rewilded valley, told without narration and with an unusual amount of patience.",
            rating = 8.3,
            durationMinutes = 82,
            genres = listOf("Documentary", "Family"),
            cast = listOf("Narrated by Ada Mensah"),
            director = "Rui Almeida",
            certification = "G",
            addedAtUtcMs = daysAgo(8),
        ),
        Movie(
            id = "mv-solar-drift",
            title = "Solar Drift",
            year = 2026,
            plot = "The crew of a solar-sail freighter wakes to find their route rewritten and their cargo singing. A strange, warm-hearted voyage about listening.",
            rating = 7.7,
            durationMinutes = 141,
            genres = listOf("Sci-Fi", "Adventure"),
            cast = listOf("Talia Brooks", "Ravi Nair", "Sena Okafor"),
            director = "Lena Marchetti",
            certification = "PG-13",
            addedAtUtcMs = daysAgo(0),
        ),
        Movie(
            id = "mv-echoes-of-salt",
            title = "Echoes of Salt",
            year = 2019,
            plot = "Three generations return to a shuttered salt works to settle an estate and discover that the family ledger is missing a page.",
            rating = 7.1,
            durationMinutes = 117,
            genres = listOf("Drama"),
            cast = listOf("Carmen Vidal", "Nils Berg", "Fatima Zahra"),
            director = "Clara Voss",
            certification = "PG-13",
            addedAtUtcMs = daysAgo(45),
        ),
        Movie(
            id = "mv-tin-roof-summer",
            title = "Tin Roof Summer",
            year = 2024,
            plot = "A failing bed-and-breakfast gets one improbable season to survive, thanks to a guest who refuses to leave and a town that refuses to help.",
            rating = 6.8,
            durationMinutes = 93,
            genres = listOf("Comedy"),
            cast = listOf("Bea Hollis", "Andre Kim", "Rosa Neri"),
            director = "Sofia Park",
            certification = "PG",
            addedAtUtcMs = daysAgo(6),
        ),
        Movie(
            id = "mv-final-departure",
            title = "Final Departure",
            year = 2022,
            plot = "An airport investigator has six hours to find a passenger who never boarded, before the flight list is sealed for good.",
            rating = 7.5,
            durationMinutes = 110,
            genres = listOf("Thriller", "Crime"),
            cast = listOf("Iris Lang", "Dmitri Volkov", "Nia Boateng"),
            director = "Owen Battersea",
            certification = "R",
            addedAtUtcMs = daysAgo(14),
        ),
    )

    private fun seriesSummary(
        id: String,
        title: String,
        year: Int,
        rating: Double,
        genres: List<String>,
        seasonCount: Int,
        addedDaysAgo: Int,
    ) = SeriesSummary(
        id = id,
        title = title,
        year = year,
        rating = rating,
        seasonCount = seasonCount,
        genres = genres,
        addedAtUtcMs = daysAgo(addedDaysAgo),
    )

    val seriesDetails: List<SeriesDetail> = listOf(
        SeriesDetail(
            summary = seriesSummary("sr-harbour-lights", "Harbour Lights", 2023, 8.0, listOf("Drama"), 2, 4),
            plot = "A harbour master, a ferry captain and a small island community navigate a year of closures, reunions and weather that refuses to cooperate.",
            cast = listOf("Nora Lind", "Samir Haddad", "Elena Rossi"),
            certification = "TV-14",
            seasons = listOf(
                season(1, "Harbour Lights", "The Crossing", "Low Tide", "Safe Passage"),
                season(2, "Harbour Lights", "Winter Schedule", "The Long Way Round"),
            ),
        ),
        SeriesDetail(
            summary = seriesSummary("sr-cartographer", "The Cartographer", 2024, 8.4, listOf("Crime", "Drama"), 1, 1),
            plot = "A mapmaker notices that a coastal town keeps appearing on new editions in slightly different places. Her investigation uncovers a quiet, decades-long fraud.",
            cast = listOf("Priya Raman", "Marcus Bell", "Ines Duarte"),
            certification = "TV-MA",
            seasons = listOf(
                season(1, "The Cartographer", "First Draft", "Scale", "Legend", "North"),
            ),
        ),
        SeriesDetail(
            summary = seriesSummary("sr-small-hours", "Small Hours", 2022, 7.6, listOf("Comedy"), 3, 9),
            plot = "The overnight staff of a 24-hour laundrette become an accidental family, one lost sock and bad decision at a time.",
            cast = listOf("Bea Hollis", "Andre Kim", "Omar Faye"),
            certification = "TV-14",
            seasons = listOf(
                season(1, "Small Hours", "Spin Cycle", "Delicates", "Permanent Press"),
                season(2, "Small Hours", "Express", "Hand Wash", "Lost and Found"),
                season(3, "Small Hours", "Last Load", "Closing Time"),
            ),
        ),
        SeriesDetail(
            summary = seriesSummary("sr-blue-ridge", "Blue Ridge", 2025, 7.9, listOf("Drama", "Crime"), 2, 0),
            plot = "A ranger and a local reporter investigate a string of disappearances along a mountain trail that officially closed years ago.",
            cast = listOf("Talia Brooks", "Elias Nord", "Rhea Solano"),
            certification = "TV-MA",
            seasons = listOf(
                season(1, "Blue Ridge", "Trailhead", "Switchback", "Overlook"),
                season(2, "Blue Ridge", "Firebreak", "Ridge Line"),
            ),
        ),
        SeriesDetail(
            summary = seriesSummary("sr-orbit-house", "Orbit House", 2021, 8.2, listOf("Documentary"), 1, 22),
            plot = "A warm, detailed look at the engineers, cooks and medics who support a small orbital research station.",
            cast = listOf("The Orbit House crew"),
            certification = "TV-PG",
            seasons = listOf(
                season(1, "Orbit House", "Launch Window", "Six Hours of Sun", "Re-entry"),
            ),
        ),
        SeriesDetail(
            summary = seriesSummary("sr-kitchen-shift", "Kitchen Shift", 2024, 7.3, listOf("Comedy"), 2, 7),
            plot = "A restaurant opens in a former bank vault, and the staff discover that the building has opinions about the menu.",
            cast = listOf("Rosa Neri", "Kenji Mori", "Mia Chen"),
            certification = "TV-14",
            seasons = listOf(
                season(1, "Kitchen Shift", "Soft Opening", "The Vault", "Service"),
                season(2, "Kitchen Shift", "New Menu", "Double Shift"),
            ),
        ),
    )

    private fun season(number: Int, seriesTitle: String, vararg titles: String): Season =
        Season(
            number = number,
            name = "Season $number",
            episodes = titles.mapIndexed { index, title ->
                Episode(
                    id = "ep-${seriesTitle.lowercase().replace(" ", "-")}-$number-${index + 1}",
                    seriesId = "sr-${seriesTitle.lowercase().replace(" ", "-")}",
                    seasonNumber = number,
                    episodeNumber = index + 1,
                    title = title,
                    plot = "An episode of $seriesTitle. Local demo metadata generated for the foundation milestone.",
                    durationMinutes = 38 + ((index * 7) % 15),
                    airDateUtcMs = daysAgo(120 - (number * 20) - index * 7),
                    stillUrl = null,
                )
            },
        )

    val channels: List<LiveChannel> = listOf(
        channel(101, "Sunset News", "news", true),
        channel(102, "World Report", "news", true),
        channel(201, "Arena Sports", "sports", false),
        channel(202, "Extreme Sports", "sports", false),
        channel(301, "Sunset Cinema", "movies", true),
        channel(302, "Classic Screen", "movies", true),
        channel(401, "Good Company", "entertainment", false),
        channel(402, "Late Night", "entertainment", false),
        channel(501, "Kids Cove", "kids", false),
        channel(502, "Little Learners", "kids", false),
    )

    private fun channel(number: Int, name: String, category: String, catchUp: Boolean) = LiveChannel(
        id = "ch-$number",
        name = name,
        number = number,
        categoryIds = listOf("live-$category"),
        hasCatchUp = catchUp,
        catchUpDays = if (catchUp) 3 else 0,
    )

    private val programmeTitlesByCategory = mapOf(
        "news" to listOf("Evening News", "The Lead", "World at Six", "Night Report", "Morning Brief"),
        "sports" to listOf("Matchday Live", "The Sports Desk", "Highlights", "Away Game", "Final Whistle"),
        "movies" to listOf("Amber Horizon", "Night Signal", "Copper Sky", "The Last Lighthouse"),
        "entertainment" to listOf("Good Company", "The Late Table", "Studio Sessions", "Open Mic"),
        "kids" to listOf("Kids Cove", "Little Explorers", "Story Hour", "Cartoon Club"),
    )

    /**
     * Generates a full day of EPG per channel, aligned to 30 minutes in the device timezone.
     * Times are stored as UTC epoch millis; the UI formats them locally.
     */
    fun guide(days: Int = 2): List<com.markooverhaul.sunset.core.model.EpgProgramme> {
        val result = mutableListOf<com.markooverhaul.sunset.core.model.EpgProgramme>()
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        for (day in 0 until days.coerceAtLeast(1)) {
            val dayStart = startOfToday + day * DAY
            channels.forEach { channel ->
                val category = channel.categoryIds.firstOrNull()?.removePrefix("live-") ?: "news"
                val titles = programmeTitlesByCategory[category] ?: programmeTitlesByCategory.getValue("news")
                var cursor = dayStart
                var index = 0
                while (cursor < dayStart + DAY) {
                    val duration = if (index % 3 == 2) 90 * MINUTE else 60 * MINUTE
                    val end = minOf(cursor + duration, dayStart + DAY)
                    val title = titles[(index + channel.number) % titles.size]
                    result += com.markooverhaul.sunset.core.model.EpgProgramme(
                        id = "epg-${channel.id}-$day-$index",
                        channelId = channel.id,
                        title = title,
                        description = "$title on ${channel.name}. Local demo guide data for the foundation milestone.",
                        startUtcMs = cursor,
                        endUtcMs = end,
                        category = category.replaceFirstChar { it.uppercase() },
                        rating = if (category == "kids") "G" else "PG",
                        catchUpRef = if (channel.hasCatchUp && end < System.currentTimeMillis()) {
                            "demo-catchup-${channel.id}-$day-$index"
                        } else {
                            null
                        },
                    )
                    cursor = end
                    index++
                }
            }
        }
        return result
    }

    fun findMovie(id: String): Movie? = movies.firstOrNull { it.id == id }

    fun findSeries(id: String): SeriesDetail? = seriesDetails.firstOrNull { it.summary.id == id }

    fun allSeriesSummaries(): List<SeriesSummary> = seriesDetails.map { it.summary }

    fun genreToCategoryId(genre: String, type: CategoryType): String? {
        val name = genre.trim().lowercase()
        return when (type) {
            CategoryType.MOVIE -> movieCategories.firstOrNull { it.name.lowercase() == name }?.id
            CategoryType.SERIES -> seriesCategories.firstOrNull { it.name.lowercase() == name }?.id
            CategoryType.LIVE -> liveCategories.firstOrNull { it.name.lowercase() == name }?.id
        }
    }
}
