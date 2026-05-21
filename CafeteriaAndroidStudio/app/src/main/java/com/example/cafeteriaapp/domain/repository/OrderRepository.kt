package com.example.cafeteriaapp.domain.repository

import com.example.cafeteriaapp.domain.model.Order
import com.example.cafeteriaapp.domain.model.OrderStatus
import kotlinx.coroutines.flow.Flow

interface OrderRepository {

    fun getActiveOrders(): Flow<List<Order>>

    fun getOrderById(orderId: String): Flow<Order?>

    fun getOrderHistoryByUserId(userId: String): Flow<List<Order>>

    suspend fun createOrder(order: Order): Boolean

    suspend fun updateOrderStatus(orderId: String, newStatus: OrderStatus)
}