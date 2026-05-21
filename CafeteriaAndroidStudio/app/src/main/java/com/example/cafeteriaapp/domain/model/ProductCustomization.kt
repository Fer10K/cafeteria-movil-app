package com.example.cafeteriaapp.domain.model

data class CustomizationOption(
    val id: String,
    val name: String,
    val extraPrice: Double = 0.0
)

data class ProductCustomization(
    val title: String,
    val isRequired: Boolean = false,
    val options: List<CustomizationOption>
) {
}