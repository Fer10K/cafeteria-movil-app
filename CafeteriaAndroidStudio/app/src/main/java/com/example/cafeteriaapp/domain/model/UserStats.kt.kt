package com.example.cafeteriaapp.domain.model

import android.provider.ContactsContract

enum class BaristaRange (val displayName: String){
    NOVICE("Amante del Café Instantáneo"),
    APPRENTICE("Aprendiz de Barista"),
    ROASTER("Maestro Tostador"),
    LEGEND("Leyenda del Espresso")
}

data class UserStats(
    val userId: String,
    val nickname: String,
    val avatarUrl: String? = null,
    val totalXp: Int,
    val currentStreak: Int,
    val achievements: List<Achievement> = emptyList()
){
    val currentRange: BaristaRange get() = when {
        totalXp < 500 -> BaristaRange.NOVICE
        totalXp < 1500 -> BaristaRange.APPRENTICE
        totalXp < 3500 -> BaristaRange.ROASTER
        else -> BaristaRange.LEGEND
    }

    val xpNextLevel: Int get() = when(currentRange){
        BaristaRange.NOVICE -> 500
        BaristaRange.APPRENTICE -> 1500
        BaristaRange.ROASTER -> 3500
        BaristaRange.LEGEND -> totalXp
    }
}