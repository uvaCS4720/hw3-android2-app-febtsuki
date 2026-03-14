package edu.nd.pmcburne.hwapp.one.data.remote

import com.google.gson.annotations.SerializedName

data class ScoreboardResponse(
    @SerializedName("games")
    val games: List<GameWrapper> = emptyList()
)

data class GameWrapper(
    @SerializedName("game")
    val game: GameDto
)

data class GameDto(
    @SerializedName("gameID")
    val gameId: String,
    val away: TeamDto,
    val home: TeamDto,
    val startTime: String = "",
    val startTimeEpoch: String = "0",
    val gameState: String = "",
    val currentPeriod: String = "",
    val contestClock: String = "",
    val finalMessage: String = ""
)

data class TeamDto(
    val score: String = "",
    val winner: Boolean = false,
    val names: TeamNamesDto
)

data class TeamNamesDto(
    val short: String = ""
)