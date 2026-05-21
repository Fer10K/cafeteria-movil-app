package com.example.cafeteriaapp.domain.model

import java.util.Date
import java.util.UUID

data class OrderItem(
    val id: String= UUID.randomUUID().toString(),
    val product: Product,
    val quantity: Int,
    val selectedCustomizations: List<String> = emptyList(),
    val subtotal: Double
)

data class Order(
    val id: String = UUID.randomUUID().toString(),
    val userId: String,
    val userNickName: String,
    val items: List<OrderItem>,
    val total: Double,
    val paymentMethod: PaymentMethod,
    val status: OrderStatus = OrderStatus.ENTRANTE,
    val scheduledPickupTime: String,
    val createdAt: Long = System.currentTimeMillis()
)