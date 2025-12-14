package com.example.ensenando.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.ensenando.data.local.dao.*
import com.example.ensenando.data.local.entity.*

@Database(
    entities = [
        UsuarioEntity::class,
        GestoEntity::class,
        UsuarioGestoEntity::class,
        DocenteEstudianteEntity::class,
        LogroEntity::class,
        UsuarioLogroEntity::class,
        HistorialIntentoEntity::class,
        ConfigEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun usuarioDao(): UsuarioDao
    abstract fun gestoDao(): GestoDao
    abstract fun usuarioGestoDao(): UsuarioGestoDao
    abstract fun docenteEstudianteDao(): DocenteEstudianteDao
    abstract fun logroDao(): LogroDao
    abstract fun usuarioLogroDao(): UsuarioLogroDao
    abstract fun historialIntentoDao(): HistorialIntentoDao
    abstract fun configDao(): ConfigDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Crear tabla historial_intentos
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS historial_intentos (
                        id_historial INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        id_usuario INTEGER NOT NULL,
                        id_gesto INTEGER NOT NULL,
                        porcentaje_obtenido INTEGER NOT NULL,
                        fecha_intento TEXT NOT NULL,
                        sync_status TEXT NOT NULL DEFAULT 'pending',
                        FOREIGN KEY(id_usuario) REFERENCES usuarios(id_usuario) ON DELETE CASCADE,
                        FOREIGN KEY(id_gesto) REFERENCES gestos(id_gesto) ON DELETE CASCADE
                    )
                """.trimIndent())
                
                // Crear índices para historial_intentos
                database.execSQL("CREATE INDEX IF NOT EXISTS index_historial_intentos_id_usuario ON historial_intentos(id_usuario)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_historial_intentos_id_gesto ON historial_intentos(id_gesto)")
                database.execSQL("CREATE INDEX IF NOT EXISTS index_historial_intentos_sync_status ON historial_intentos(sync_status)")
                
                // Crear tabla config
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS config (
                        clave TEXT PRIMARY KEY NOT NULL,
                        valor TEXT NOT NULL
                    )
                """.trimIndent())
                
                // Insertar valores por defecto
                database.execSQL("INSERT OR IGNORE INTO config (clave, valor) VALUES ('tema', 'auto')")
                database.execSQL("INSERT OR IGNORE INTO config (clave, valor) VALUES ('notificaciones_logros', 'true')")
                database.execSQL("INSERT OR IGNORE INTO config (clave, valor) VALUES ('notificaciones_solicitudes', 'true')")
                database.execSQL("INSERT OR IGNORE INTO config (clave, valor) VALUES ('recordatorios', 'false')")
            }
        }
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "lengua_senas_database"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}


