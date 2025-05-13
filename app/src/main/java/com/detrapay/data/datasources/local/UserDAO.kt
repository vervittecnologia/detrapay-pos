package com.detrapay.data.datasources.local

import androidx.room.*
import com.detrapay.data.model.local.User

@Dao
interface UsersDao {
    //for single user insert
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User): Long

    //for list of users insert
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserAll(users: List<User>): List<Long>

    @Update
    suspend fun updateUser(user:User)

    //getting all users
    @Query("select * from users")
    suspend fun getUsers(): List<User>

    //getting user data details
    @Query("select * from users where id Like :id")
    suspend fun getUserDataDetails(id: Long): User

    //deleting all user from db
    @Query("DELETE FROM users")
    suspend fun deleteAll()
}
