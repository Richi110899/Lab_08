package com.example.lab08

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ColumnInfo
import java.util.Date

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "title")
    val title: String,

    @ColumnInfo(name = "description")
    val description: String = "",

    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,

    @ColumnInfo(name = "priority")
    val priority: Priority = Priority.MEDIUM,

    @ColumnInfo(name = "due_date")
    val dueDate: Date? = null,

    @ColumnInfo(name = "category")
    val category: String = DEFAULT_CATEGORY,

    @ColumnInfo(name = "created_at")
    val createdAt: Date = Date(),

    @ColumnInfo(name = "is_recurring")
    val isRecurring: Boolean = false,

    @ColumnInfo(name = "recurring_interval")
    val recurringInterval: Int = 0,

    @ColumnInfo(name = "last_modified")
    val lastModified: Date = Date()
) {
    companion object {
        const val DEFAULT_CATEGORY = ""
    }
}

enum class Priority {
    LOW, MEDIUM, HIGH
}

enum class TaskFilter {
    ALL, TODAY, UPCOMING, COMPLETED, PRIORITY
}
