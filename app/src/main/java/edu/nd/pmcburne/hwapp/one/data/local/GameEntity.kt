package edu.nd.pmcburne.hwapp.one.data.local

import androidx.room.Entity

@Entity(
    tableName = "games",
    primaryKeys = ["gameId", "gender", "gameDate"]
)
data class GameEntity(
    val gameId: String,
    val gender: String,
    val gameDate: String,
    val startTimeEpoch: Long,
    val startTimeText: String,
    val gameState: String,
    val awayTeam: String,
    val homeTeam: String,
    val awayScore: Int?,
    val homeScore: Int?,
    val currentPeriod: String,
    val contestClock: String,
    val awayWinner: Boolean,
    val homeWinner: Boolean
)