package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val filePath: String,
    val timestamp: Long,
    val durationMs: Long,
    val presetName: String,
    val pitch: Float,
    val speed: Float,
    val isDenoised: Boolean,
    val backgroundSound: String
)
