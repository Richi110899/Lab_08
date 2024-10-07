package com.example.lab08

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.room.Room
import com.example.lab08.ui.theme.Lab08Theme
import androidx.core.app.NotificationCompat

object DatabaseProvider {
    @Volatile
    private var INSTANCE: TaskDatabase? = null

    fun getDatabase(context: Context): TaskDatabase {
        return INSTANCE ?: synchronized(this) {
            val instance = Room.databaseBuilder(
                context.applicationContext,
                TaskDatabase::class.java,
                "task_db8"
            ).build()
            INSTANCE = instance
            instance
        }
    }
}

class MainActivity : ComponentActivity() {
    private val CHANNEL_ID = "task_reminders_channel"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Lab08Theme {
                val db = DatabaseProvider.getDatabase(applicationContext)
                val taskDao = db.taskDao()
                val viewModel = TaskViewModel(taskDao)

                // Inicializar el canal de notificación
                createNotificationChannel()

                val notifiedTasks = remember { mutableSetOf<Int>() }

                LaunchedEffect(Unit) {
                    viewModel.notificationEvents.collect { task ->
                        sendNotification(task)
                    }
                }

                TodoistStyle(viewModel)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Recordatorios de Tareas",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Canal para notificaciones de tareas pendientes"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }


    private fun sendNotification(task: Task) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification) // Cambia esto a tu ícono
            .setContentTitle("Tarea Pendiente")
            .setContentText("Tienes una tarea pendiente: ${task.title}")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(task.id, notification)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoistStyle(viewModel: TaskViewModel) {
    var showAddTask by remember { mutableStateOf(false) }
    var showEditTask by remember { mutableStateOf<Pair<Task, Boolean>?>(null) }
    var selectedFilter by remember { mutableStateOf(TaskFilter.ALL) }

    // Observa la lista de tareas
    val tasks by viewModel.tasks.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tareas") },
                actions = {
                    IconButton(onClick = { /* Implementar búsqueda */ }) {
                        Icon(Icons.Default.Search, contentDescription = "Buscar")
                    }
                    IconButton(onClick = { /* Implementar más opciones */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
                    }
                }
            )
        },
        floatingActionButton = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (tasks.isNotEmpty()) {
                    FloatingActionButton(
                        onClick = { viewModel.deleteAllTasks() },
                        containerColor = Color.Yellow
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar todas las tareas")
                    }
                }

                FloatingActionButton(
                    onClick = { showAddTask = true },
                    containerColor = Color.Green
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Agregar tarea")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            ScrollableTabRow(
                selectedTabIndex = TaskFilter.entries.indexOf(selectedFilter),
                modifier = Modifier.fillMaxWidth()
            ) {
                TaskFilter.entries.forEach { filter ->
                    Tab(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        text = { Text(filter.name) }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp)
            ) {
                items(tasks) { task ->
                    TaskItemCard(
                        task = task,
                        onCompleteTask = { viewModel.updateTask(task.copy(isCompleted = !task.isCompleted)) },
                        onDeleteTask = { viewModel.deleteTask(task) },
                        onEditTask = { showEditTask = task to true }
                    )
                }
            }
        }
    }

    // Diálogo para agregar tareas
    if (showAddTask) {
        AddTaskDialog(
            onDismiss = { showAddTask = false },
            onTaskAdded = { title, priority, category ->
                viewModel.addTask(title, priority, category)
                showAddTask = false
            }
        )
    }

    // Diálogo para editar tareas
    showEditTask?.let { (task, _) ->
        EditTaskDialog(
            task = task,
            onDismiss = { showEditTask = null },
            onTaskUpdated = { title, priority, category ->
                viewModel.updateTask(task.copy(title = title, priority = priority, category = category))
                showEditTask = null
            }
        )
    }
}

@Composable
fun TaskItemCard(
    task: Task,
    onCompleteTask: () -> Unit,
    onDeleteTask: () -> Unit,
    onEditTask: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onCompleteTask() }
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
            ) {
                Text(
                    text = task.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                if (task.description.isNotEmpty()) {
                    Text(
                        text = task.description,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(9.dp)
                    .background(
                        color = when (task.priority) {
                            Priority.HIGH -> Color.Red
                            Priority.MEDIUM -> Color.Yellow
                            Priority.LOW -> Color.Green
                        },
                        shape = CircleShape
                    )
            )

            IconButton(onClick = onEditTask) {
                Icon(Icons.Default.Edit, contentDescription = "Editar")
            }

            IconButton(onClick = onDeleteTask) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar")
            }
        }
    }
}

@Composable
fun AddTaskDialog(
    onDismiss: () -> Unit,
    onTaskAdded: (String, Priority, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf(Priority.MEDIUM) }
    var category by remember { mutableStateOf(Task.DEFAULT_CATEGORY) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva tarea") },
        text = {
            Column {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Prioridad:", modifier = Modifier.padding(top = 8.dp))

                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Priority.entries.forEach { p ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = priority == p,
                                onClick = { priority = p }
                            )
                            Text(p.name)
                        }
                    }
                }

                TextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Categoría") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotEmpty()) {
                        onTaskAdded(title, priority, category)
                    }
                }
            ) {
                Text("Agregar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
fun EditTaskDialog(
    task: Task,
    onDismiss: () -> Unit,
    onTaskUpdated: (String, Priority, String) -> Unit
) {
    var title by remember { mutableStateOf(task.title) }
    var priority by remember { mutableStateOf(task.priority) }
    var category by remember { mutableStateOf(task.category) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Modificar tarea") },
        text = {
            Column {
                TextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Título") },
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Prioridad:", modifier = Modifier.padding(top = 8.dp))

                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Priority.entries.forEach { p ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = priority == p,
                                onClick = { priority = p }
                            )
                            Text(p.name)
                        }
                    }
                }

                TextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Categoría") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (title.isNotEmpty()) {
                        onTaskUpdated(title, priority, category)
                    }
                }
            ) {
                Text("Modificar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
