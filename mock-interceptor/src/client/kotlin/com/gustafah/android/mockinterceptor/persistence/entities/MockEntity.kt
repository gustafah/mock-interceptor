package com.gustafah.android.mockinterceptor.persistence.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(primaryKeys = ["id", "file_name"])
data class MockEntity(
    @ColumnInfo(name = "id", defaultValue = "")
    val id: String,
    @ColumnInfo(name = "file_name")
    val fileName: String,
    @ColumnInfo(name = "file_data")
    val fileData: String
)