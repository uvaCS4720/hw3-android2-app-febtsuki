package edu.nd.pmcburne.hwapp.one.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameDao {
    @Query(
        """
        SELECT * FROM games
        WHERE gender = :gender AND gameDate = :gameDate
        ORDER BY startTimeEpoch ASC, gameId ASC
        """
    )
    fun observeGames(gender: String, gameDate: String): Flow<List<GameEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(games: List<GameEntity>)

    @Query("DELETE FROM games WHERE gender = :gender AND gameDate = :gameDate")
    suspend fun deleteGamesForSelection(gender: String, gameDate: String)
}