package com.example.lab08

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Date

class TaskViewModel(private val dao: TaskDao) : ViewModel() {
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    private val _notificationEvents = MutableSharedFlow<Task>()
    val notificationEvents: SharedFlow<Task> = _notificationEvents.asSharedFlow()

    init {
        loadTasks()
        observeNotificationEvents() // Suscribirse a eventos de notificación
    }

    private fun loadTasks() {
        viewModelScope.launch {
            dao.getAllTasksFlow().collect { taskList ->
                _tasks.value = taskList
            }
        }
    }

    fun addTask(title: String, priority: Priority = Priority.MEDIUM, category: String = Task.DEFAULT_CATEGORY) {
        if (title.isNotBlank()) { // Asegúrate de que el título no esté vacío
            viewModelScope.launch {
                val newTask = Task(
                    title = title,
                    priority = priority,
                    category = category,
                    createdAt = Date()
                )
                dao.insertTask(newTask)
                _notificationEvents.emit(newTask) // Emitir el evento de notificación
            }
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            dao.deleteTask(task)
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            dao.updateTask(task)
            // Emitir el evento de notificación solo si la tarea no está completada
            if (!task.isCompleted) {
                Log.d("TaskViewModel", "Notificación enviada para: ${task.title}")
                _notificationEvents.emit(task)
            }
        }
    }

    fun deleteAllTasks() {
        viewModelScope.launch {
            dao.deleteAllTasks()
            _tasks.value = emptyList() // Vaciamos la lista en el estado
        }
    }

    // Función para observar eventos de notificación
    private fun observeNotificationEvents() {
        viewModelScope.launch {
            notificationEvents.collect { task ->
                // Aquí puedes manejar la lógica de notificación
                // Esto podría ser un evento que llame a sendNotification en MainActivity
            }
        }
    }
}
