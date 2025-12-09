package com.example.ensenando.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.ensenando.data.local.dao.*
import com.example.ensenando.data.local.entity.*

@Database(
    entities = [
        UsuarioEntity::class,
        GestoEntity::class,
        UsuarioGestoEntity::class,
        DocenteEstudianteEntity::class,
        LogroEntity::class,
        UsuarioLogroEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun gestoDao(): GestoDao
    abstract fun usuarioGestoDao(): UsuarioGestoDao
    abstract fun docenteEstudianteDao(): DocenteEstudianteDao
    abstract fun logroDao(): LogroDao
    abstract fun usuarioLogroDao(): UsuarioLogroDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lengua_senas_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}


