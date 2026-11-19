
package com.example.examentpcontact.data

data class Contact(
    val id: Long = 0,
    val nom: String,
    val prenom: String,
    val telephone: String,
    val email: String? = null,
    val photoUri: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val dernierAppel: Long = 0,
    val favori: Boolean = false
)
