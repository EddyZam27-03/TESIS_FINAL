package com.example.ensenando.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.ensenando.R
import com.example.ensenando.ui.main.MainActivity

object NotificationManager {
    private const val CHANNEL_ID = "ensenando_notifications"
    private const val CHANNEL_NAME = "Notificaciones Ensenando"
    private const val NOTIFICATION_ID_LOGRO = 1000
    
    /**
     * Crear canal de notificaciones (requerido en Android 8.0+)
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notificaciones de logros, solicitudes y recordatorios"
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Mostrar notificación de logro desbloqueado
     */
    fun mostrarNotificacionLogro(context: Context, titulo: String, descripcion: String) {
        createNotificationChannel(context)
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("¡Logro Desbloqueado!")
            .setContentText(titulo)
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("$titulo\n$descripcion"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_LOGRO + System.currentTimeMillis().toInt(), notification)
    }
    
    /**
     * Mostrar notificación de solicitud recibida (para docentes)
     */
    fun mostrarNotificacionSolicitud(context: Context, nombreEstudiante: String) {
        createNotificationChannel(context)
        
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("fragment", "profile")
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Nueva Solicitud")
            .setContentText("$nombreEstudiante quiere vincularse contigo")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID_LOGRO + System.currentTimeMillis().toInt(), notification)
    }
    
    /**
     * Mostrar Toast (alternativa simple a notificación)
     */
    fun mostrarToastLogro(context: Context, titulo: String) {
        android.widget.Toast.makeText(
            context,
            "¡Has obtenido el logro: $titulo!",
            android.widget.Toast.LENGTH_LONG
        ).show()
    }
}
