package com.example.cafeteriaapp.domain.repository

import com.example.cafeteriaapp.domain.model.Achievement
import com.example.cafeteriaapp.domain.model.UserStats
import kotlinx.coroutines.flow.Flow

interface GamificationRepository {

    fun getUserStats(userId: String): Flow<UserStats?>

    fun getLeaderboard(): Flow<List<UserStats>>

    suspend fun awardXp(userId: String, amount: Int)

    suspend fun unlockAchievement(userId: String, achievementId: String)

    suspend fun updateStreak(userId: String, increment: Boolean)
}