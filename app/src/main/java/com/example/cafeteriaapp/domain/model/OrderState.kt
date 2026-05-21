package com.example.cafeteriaapp.domain.model

enum class OrderStatus {
    ENTRANTE,
    EN_PROCESO,
    LISTO,
    ENTREGADO,
    CANCELADO
}

enum class PaymentMethod{
    NFC,
    EFECTIVO
}