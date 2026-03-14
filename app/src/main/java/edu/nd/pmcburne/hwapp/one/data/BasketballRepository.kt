package edu.nd.pmcburne.hwapp.one.data

import edu.nd.pmcburne.hwapp.one.data.local.GameDao
import edu.nd.pmcburne.hwapp.one.data.local.GameEntity
import edu.nd.pmcburne.hwapp.one.data.remote.ScoreApi
import kotlinx.coroutines.flow.Flow
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.time.LocalDate

class BasketballRepository(
    private val api: ScoreApi,
    private val gameDao: GameDao
) {
    fun observeGames(gender: String, date: LocalDate): Flow<List<GameEntity>> {
        return gameDao.observeGames(gender = gender, gameDate = date.toString())
    }

    suspend fun refreshGames(gender: String, date: LocalDate) {
        val response = api.getScores(
            gender = gender,
            year = date.year.toString(),
            month = date.monthValue.toString().padStart(2, '0'),
            day = date.dayOfMonth.toString().padStart(2, '0')
        )

        val games = response.games.map { wrapper ->
            val game = wrapper.game
            GameEntity(
                gameId = game.gameId,
                gender = gender,
                gameDate = date.toString(),
                startTimeEpoch = game.startTimeEpoch.toLongOrNull() ?: 0L,
                startTimeText = game.startTime,
                gameState = game.gameState,
                awayTeam = game.away.names.short,
                homeTeam = game.home.names.short,
                awayScore = game.away.score.toIntOrNull(),
                homeScore = game.home.score.toIntOrNull(),
                currentPeriod = game.currentPeriod,
                contestClock = game.contestClock,
                awayWinner = game.away.winner,
                homeWinner = game.home.winner
            )
        }

        gameDao.deleteGamesForSelection(gender = gender, gameDate = date.toString())
        gameDao.insertAll(games)
    }

    companion object {
        fun create(gameDao: GameDao): BasketballRepository {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }
            val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl("https://ncaa-api.henrygd.me/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            return BasketballRepository(
                api = retrofit.create(ScoreApi::class.java),
                gameDao = gameDao
            )
        }
    }
}