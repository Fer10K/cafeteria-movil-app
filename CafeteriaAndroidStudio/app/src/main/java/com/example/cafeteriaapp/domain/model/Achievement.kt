package com.example.cafeteriaapp.domain.model

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val iconUrl: String?,
    val xpReward: Int,
    val isUnlocked: Boolean = false,
    val unlocked: Long? = null
)