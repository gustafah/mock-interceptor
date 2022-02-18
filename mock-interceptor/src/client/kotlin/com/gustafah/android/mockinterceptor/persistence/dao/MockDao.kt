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

    @Query("SELECT * FROM mockentity WHERE file_name = :fileName")
    fun findMock(fileName: String) : MockEntity?

    @Query("SELECT * FROM mockentity")
    fun getAllMocks() : List<MockEntity>?

}