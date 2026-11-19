package com.example.examentpcontact

import android.content.Context
import android.graphics.Color
import androidx.preference.PreferenceManager

class PreferencesManager(context: Context) {

    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    // Taille en sp (défaut 16)
    fun tailleSp(): Float =
        prefs.getString("taille_police", "16")?.toFloatOrNull() ?: 16f

    // Couleur convertie en Int (défaut noir)
    fun couleur(): Int = try {
        Color.parseColor(prefs.getString("couleur_preferee", "#000000"))
    } catch (e: IllegalArgumentException) {
        Color.BLACK
    }

    // Bouton Appeler visible dans la liste ? (défaut non)
    fun appelActif(): Boolean = prefs.getBoolean("appel_depuis_liste", false)

}