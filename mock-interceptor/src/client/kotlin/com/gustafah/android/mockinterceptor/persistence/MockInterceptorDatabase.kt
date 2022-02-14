package com.gustafah.android.mockinterceptor.persistence

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.gustafah.android.mockinterceptor.persistence.dao.MockDao
import com.gustafah.android.mockinterceptor.persistence.entities.MockEntity
import java.io.File

@Database(
    version = MockInterceptorDatabase.VERSION,
    exportSchema = true,
    entities = [MockEntity::class]
)
internal abstract class MockInterceptorDatabase : RoomDatabase() {

    companion object {
        const val NAME = "mock-interceptor.db"
        const val VERSION = 1

        private var instance: MockInterceptorDatabase? = null

        fun getInstance(context: Context, file: File? = null): MockInterceptorDatabase {
            if (instance == null || file != null) {
                instance?.close()
                val roomDatabase = Room
                    .databaseBuilder(
                        context,
                        MockInterceptorDatabase::class.java, NAME
                    )
                    .setJournalMode(JournalMode.TRUNCATE)
                file?.let { roomDatabase.createFromFile(it) }

                instance = roomDatabase.build()
            }
            return instance!!
        }
    }

    abstract fun mockDao(): MockDao

}