package com.app.lab01.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.lab01.data.TareaDatabase
import com.app.lab01.data.TareaEntity
import com.app.lab01.notifications.NotificationScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TareasViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = TareaDatabase.getInstance(application).tareaDao()
    private val ctx = application.applicationContext

    val tareas: StateFlow<List<TareaEntity>> = dao.obtenerTodas()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun agregarTarea(titulo: String, fechaLimite: Long? = null) {
        viewModelScope.launch {
            val nueva = TareaEntity(titulo = titulo, fechaLimite = fechaLimite)
            dao.insertar(nueva)
            // Reprogramamos con el id real leyendo la última tarea insertada
            val insertada = dao.obtenerTodas().stateIn(viewModelScope).value.lastOrNull()
            insertada?.let { NotificationScheduler.programar(ctx, it) }
        }
    }

    fun toggleTarea(tarea: TareaEntity) {
        viewModelScope.launch {
            val actualizada = tarea.copy(completada = !tarea.completada)
            dao.actualizar(actualizada)
            // Si se completó, cancelar notificaciones pendientes
            if (actualizada.completada) NotificationScheduler.cancelar(ctx, tarea)
        }
    }

    fun eliminarTarea(tarea: TareaEntity) {
        viewModelScope.launch {
            NotificationScheduler.cancelar(ctx, tarea)
            dao.eliminar(tarea)
        }
    }

    fun editarTarea(tarea: TareaEntity, nuevoTitulo: String, nuevaFecha: Long?) {
        viewModelScope.launch {
            NotificationScheduler.cancelar(ctx, tarea)
            val actualizada = tarea.copy(titulo = nuevoTitulo, fechaLimite = nuevaFecha)
            dao.actualizar(actualizada)
            NotificationScheduler.programar(ctx, actualizada)
        }
    }
}