package com.gustafah.android.mockinterceptor.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.gustafah.android.mockinterceptor.persistence.entities.MockEntity

@Dao
interface MockDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMock(mockEntity: MockEntity)

    @Query("SELECT * FROM mockentity WHERE file_name = :fileName AND id = :id")
    fun findMock(id: String, fileName: String) : MockEntity?

    @Query("SELECT (id) FROM mockentity")
    suspend fun getAllIdentifiers() : List<String>

    @Query("SELECT * FROM mockentity")
    fun getAllMocks() : List<MockEntity>?

}