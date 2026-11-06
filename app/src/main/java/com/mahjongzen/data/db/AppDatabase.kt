package com.mahjongzen.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [GameRecordEntity::class, SavedGameEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun records(): GameRecordDao
    abstract fun savedGame(): SavedGameDao

    companion object {
        fun create(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "mahjong.db")
                .fallbackToDestructiveMigration()
                .build()
    }
}
