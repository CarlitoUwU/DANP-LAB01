package com.app.lab01.notifications

import android.content.Context
import androidx.work.*
import com.app.lab01.data.TareaEntity
import java.util.concurrent.TimeUnit

object NotificationScheduler {

    fun programar(context: Context, tarea: TareaEntity) {
        val fechaLimite = tarea.fechaLimite ?: return
        val ahora = System.currentTimeMillis()

        val delayDiaAntes = fechaLimite - TimeUnit.DAYS.toMillis(1) - ahora
        if (delayDiaAntes > 0) {
            encolar(context, tarea, delayDiaAntes, esDiaVencimiento = false)
        }

        val delayDiaVencimiento = fechaLimite - ahora
        if (delayDiaVencimiento > 0) {
            encolar(context, tarea, delayDiaVencimiento, esDiaVencimiento = true)
        }
    }

    fun cancelar(context: Context, tarea: TareaEntity) {
        WorkManager.getInstance(context).cancelAllWorkByTag("tarea_${tarea.id}")
    }

    private fun encolar(
        context: Context,
        tarea: TareaEntity,
        delayMs: Long,
        esDiaVencimiento: Boolean
    ) {
        val data = workDataOf(
            "titulo" to tarea.titulo,
            "esDiaVencimiento" to esDiaVencimiento
        )
        val request = OneTimeWorkRequestBuilder<TareaNotificationWorker>()
            .setInitialDelay(delayMs, TimeUnit.MILLISECONDS)
            .setInputData(data)
            .addTag("tarea_${tarea.id}")
            .build()

        WorkManager.getInstance(context)
            .enqueue(request)
    }
}