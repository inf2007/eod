package com.singaporetech.eod

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object Interface for Player records.
 * This DAO interface is used to generate a clean code-based API for your DB.
 * In other words associate SQL queries to methods calls.
 * - similarly uses kotlin annotations to create loads of boilerplate code
 * - all methods are queries and self-documenting
 */
@Dao // tells Room this is part of that
interface PlayerDAO {

    // the insert annotation doesn't even need proper SQL
    // .Ignore will ignore a new player if it has the same key
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(player: Player)

    @Query("DELETE FROM player_table")
    fun purge()

    @Query("SELECT * FROM player_table ORDER BY name ASC")
    fun getOrderedPlayerNames(): List<Player>

    // A FlowData stream from the DB
    // NOTE that Flow is a way to handle live continuous data
    // - Flow is like thread-safe LiveData
    // - emit and collect is like postValue and observe
    @Query("SELECT * FROM player_table ORDER BY name ASC")
    fun getOrderedPlayerNamesFlow(): Flow<List<Player>>

    @Query("SELECT * FROM player_table WHERE name = :name")
    fun getByName(name:String): List<Player>

    @Query("UPDATE player_table SET pw = :pw WHERE name = :name")
    fun updatePw(name: String, pw: String)
}
