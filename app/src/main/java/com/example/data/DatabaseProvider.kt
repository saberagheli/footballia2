package com.example.data

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    private var database: AppDatabase? = null
    private var repository: FootballPredictorRepository? = null

    fun getRepository(context: Context): FootballPredictorRepository {
        return repository ?: synchronized(this) {
            val db = database ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "football_predictor_db"
            )
            .fallbackToDestructiveMigration()
            .build()
            database = db
            
            val repo = FootballPredictorRepository(db.appDao())
            repository = repo
            repo
        }
    }
}
