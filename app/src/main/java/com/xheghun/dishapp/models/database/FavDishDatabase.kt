package com.xheghun.dishapp.models.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.xheghun.dishapp.models.entities.FavDish

@Database(entities = [FavDish::class], version = 1)
abstract class FavDishDatabase: RoomDatabase() {

    abstract fun favDishDao(): FavDishDao

    companion object {
        @Volatile
        private var INSTANCE: FavDishDatabase? = null

        /// create FavDishDatabase Singleton
        fun getDatabase(context: Context): FavDishDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FavDishDatabase::class.java,
                    "fav_dish_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()

                INSTANCE = instance

                instance
            }
        }
    }
}