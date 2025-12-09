package com.example.ensenando.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.ensenando.data.local.entity.UsuarioEntity
import com.example.ensenando.data.local.entity.UsuarioGestoEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerator {
    fun generarReportePDF(
        context: Context,
        usuario: UsuarioEntity,
        progresos: List<UsuarioGestoEntity>,
        gestos: Map<Int, String> = emptyMap(),
        gestosCompletos: Map<Int, com.example.ensenando.data.local.entity.GestoEntity> = emptyMap()
    ): String {
        val downloadsDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
            ?: context.filesDir
        val fileName = "reporte_${usuario.idUsuario}_${System.currentTimeMillis()}.pdf"
        val file = File(downloadsDir, fileName)
        
        val document = PdfDocument()
        val pageWidth = 595 // A4 width in points
        val pageHeight = 842 // A4 height in points
        val margin = 50f
        var currentPage = 1
        
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPage).create()
        var page = document.startPage(pageInfo)
        var canvas = page.canvas
        var y = margin
        
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 24f
            isFakeBoldText = true
        }
        
        val headerPaint = Paint().apply {
            color = Color.BLACK
            textSize = 16f
            isFakeBoldText = true
        }
        
        val textPaint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
        }
        
        val smallPaint = Paint().apply {
            color = Color.DKGRAY
            textSize = 10f
        }
        
        // Título
        canvas.drawText("REPORTE DE PROGRESO", margin, y, titlePaint)
        y += 40f
        
        // Información del usuario
        canvas.drawText("INFORMACIÓN DEL USUARIO", margin, y, headerPaint)
        y += 25f
        canvas.drawText("Nombre: ${usuario.nombre}", margin, y, textPaint)
        y += 20f
        canvas.drawText("Correo: ${usuario.correo}", margin, y, textPaint)
        y += 20f
        canvas.drawText("Rol: ${usuario.rol.uppercase()}", margin, y, textPaint)
        y += 20f
        
        val fechaFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
        canvas.drawText("Fecha de generación: ${fechaFormat.format(Date())}", margin, y, smallPaint)
        y += 40f
        
        // Estadísticas mejoradas
        canvas.drawText("ESTADÍSTICAS GENERALES", margin, y, headerPaint)
        y += 25f
        
        val totalGestos = progresos.size
        val gestosAprendidos = progresos.count { it.estado == "aprendido" && it.porcentaje >= 80 }
        val gestosEnProgreso = progresos.count { it.porcentaje > 0 && it.porcentaje < 80 }
        val gestosNoIniciados = progresos.count { it.porcentaje == 0 }
        val promedio = if (progresos.isNotEmpty()) progresos.map { it.porcentaje }.average().toInt() else 0
        val tiempoTotal = (progresos.sumOf { it.porcentaje } * 0.5).toInt()
        val porcentajeCompletitud = if (totalGestos > 0) (gestosAprendidos * 100 / totalGestos) else 0
        
        // Estadísticas básicas
        canvas.drawText("Total de gestos practicados: $totalGestos", margin, y, textPaint)
        y += 20f
        canvas.drawText("Gestos aprendidos (≥80%): $gestosAprendidos", margin, y, textPaint)
        y += 20f
        canvas.drawText("Gestos en progreso: $gestosEnProgreso", margin, y, textPaint)
        y += 20f
        canvas.drawText("Gestos no iniciados: $gestosNoIniciados", margin, y, textPaint)
        y += 20f
        canvas.drawText("Promedio de progreso: $promedio%", margin, y, textPaint)
        y += 20f
        canvas.drawText("Porcentaje de completitud: $porcentajeCompletitud%", margin, y, textPaint)
        y += 20f
        canvas.drawText("Tiempo total estimado: $tiempoTotal minutos", margin, y, textPaint)
        y += 40f
        
        // Estadísticas por categoría
        if (gestosCompletos.isNotEmpty()) {
            canvas.drawText("ESTADÍSTICAS POR CATEGORÍA", margin, y, headerPaint)
            y += 25f
            
            val categoriasMap = mutableMapOf<String, Triple<Int, Int, Int>>() // Total, Aprendidos, Promedio
            
            progresos.forEach { progreso ->
                val gesto = gestosCompletos[progreso.idGesto]
                val categoria = gesto?.categoria?.split(" - ")?.firstOrNull() ?: "Sin categoría"
                val (total, aprendidos, suma) = categoriasMap.getOrDefault(categoria, Triple(0, 0, 0))
                val nuevoAprendidos = if (progreso.estado == "aprendido" && progreso.porcentaje >= 80) aprendidos + 1 else aprendidos
                categoriasMap[categoria] = Triple(total + 1, nuevoAprendidos, suma + progreso.porcentaje)
            }
            
            categoriasMap.forEach { (categoria, stats) ->
                val (total, aprendidos, suma) = stats
                val promedioCat = if (total > 0) suma / total else 0
                
                if (y > pageHeight - 100) {
                    document.finishPage(page)
                    currentPage++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPage).create()
                    page = document.startPage(pageInfo)
                    canvas = page.canvas
                    y = margin
                }
                
                canvas.drawText("$categoria:", margin, y, headerPaint)
                y += 18f
                canvas.drawText("  Total: $total | Aprendidos: $aprendidos | Promedio: $promedioCat%", margin + 20f, y, textPaint)
                y += 20f
            }
            y += 20f
        }
        
        // Gráfico de barras simple para distribución de estados
        canvas.drawText("DISTRIBUCIÓN DE ESTADOS", margin, y, headerPaint)
        y += 30f
        
        val barWidth = 400f
        val barHeight = 20f
        val maxValue = maxOf(gestosAprendidos, gestosEnProgreso, gestosNoIniciados, 1)
        
        // Barra para aprendidos (verde)
        val aprendidosWidth = (gestosAprendidos.toFloat() / maxValue) * barWidth
        canvas.drawRect(margin, y, margin + aprendidosWidth, y + barHeight, Paint().apply {
            color = Color.parseColor("#4CAF50")
        })
        canvas.drawText("Aprendidos: $gestosAprendidos", margin + aprendidosWidth + 10f, y + 15f, textPaint)
        y += 30f
        
        // Barra para en progreso (amarillo)
        val enProgresoWidth = (gestosEnProgreso.toFloat() / maxValue) * barWidth
        canvas.drawRect(margin, y, margin + enProgresoWidth, y + barHeight, Paint().apply {
            color = Color.parseColor("#FF9800")
        })
        canvas.drawText("En progreso: $gestosEnProgreso", margin + enProgresoWidth + 10f, y + 15f, textPaint)
        y += 30f
        
        // Barra para no iniciados (gris)
        val noIniciadosWidth = (gestosNoIniciados.toFloat() / maxValue) * barWidth
        canvas.drawRect(margin, y, margin + noIniciadosWidth, y + barHeight, Paint().apply {
            color = Color.parseColor("#9E9E9E")
        })
        canvas.drawText("No iniciados: $gestosNoIniciados", margin + noIniciadosWidth + 10f, y + 15f, textPaint)
        y += 40f
        
        // Detalle de gestos
        canvas.drawText("DETALLE DE GESTOS", margin, y, headerPaint)
        y += 25f
        
        // Encabezados de tabla
        val tableY = y
        canvas.drawLine(margin, y, pageWidth - margin, y, Paint().apply { color = Color.BLACK })
        y += 5f
        canvas.drawText("ID", margin, y, headerPaint)
        canvas.drawText("Nombre", margin + 80f, y, headerPaint)
        canvas.drawText("Progreso", margin + 300f, y, headerPaint)
        canvas.drawText("Estado", margin + 400f, y, headerPaint)
        y += 20f
        canvas.drawLine(margin, y, pageWidth - margin, y, Paint().apply { color = Color.BLACK })
        y += 10f
        
        // Filas de datos
        progresos.forEach { progreso ->
            if (y > pageHeight - 100) {
                // Nueva página
                document.finishPage(page)
                currentPage++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPage).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = margin
            }
            
            val nombreGesto = gestos[progreso.idGesto] ?: "Gesto ${progreso.idGesto}"
            canvas.drawText("${progreso.idGesto}", margin, y, textPaint)
            canvas.drawText(nombreGesto, margin + 80f, y, textPaint)
            canvas.drawText("${progreso.porcentaje}%", margin + 300f, y, textPaint)
            canvas.drawText(progreso.estado, margin + 400f, y, textPaint)
            y += 20f
        }
        
        document.finishPage(page)
        document.writeTo(file.outputStream())
        document.close()
        
        return file.absolutePath
    }
}


