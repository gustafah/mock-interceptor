package com.gustafah.android.mockinterceptor.persistence.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class MockEntity(
    @PrimaryKey
    @ColumnInfo(name = "file_name")
    val fileName: String,
    @ColumnInfo(name = "file_data")
    val fileData: String
)