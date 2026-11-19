package com.example.examentpcontact

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import java.io.File
import java.io.FileOutputStream

// ---------- Messages et navigation ----------
fun Fragment.toast(message: String) {
    context?.let { Toast.makeText(it, message, Toast.LENGTH_LONG).show() }
}

fun Fragment.naviguerVers(fragment: Fragment) {
    parentFragmentManager.beginTransaction()
        .replace(R.id.container, fragment)
        .addToBackStack(null)
        .commit()
}

// ---------- Photos ----------
fun ImageView.afficherPhoto(chemin: String?) {
    if (chemin.isNullOrEmpty() || !File(chemin).exists()) {
        setImageResource(android.R.drawable.ic_menu_gallery)
    } else {
        setImageURI(Uri.fromFile(File(chemin)))
    }
}

// Copie une image de la galerie dans filesDir (réduite pour économiser la mémoire)
fun copierUri(context: Context, uri: Uri): String? = try {
    val options = BitmapFactory.Options().apply { inSampleSize = 4 }
    val bitmap = context.contentResolver.openInputStream(uri)?.use {
        BitmapFactory.decodeStream(it, null, options)
    }
    if (bitmap == null) null else sauverBitmap(context, bitmap)
} catch (e: Exception) {
    null
}

fun sauverBitmap(context: Context, bitmap: Bitmap): String? = try {
    val fichier = File(context.filesDir, "photo_${System.currentTimeMillis()}.jpg")
    FileOutputStream(fichier).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it) }
    fichier.absolutePath
} catch (e: Exception) {
    null
}

// ---------- Appel et e-mail ----------
fun appeler(context: Context, numero: String) {
    if (numero.isBlank()) {
        Toast.makeText(context, "Numéro vide", Toast.LENGTH_SHORT).show()
        return
    }
    val intent = Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", numero, null))
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "Aucune application téléphone", Toast.LENGTH_SHORT).show()
    }
}

fun envoyerEmail(context: Context, email: String) {
    if (email.isBlank()) {
        Toast.makeText(context, "E-mail vide", Toast.LENGTH_SHORT).show()
        return
    }
    val intent = Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", email, null))
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, "Aucune application e-mail", Toast.LENGTH_SHORT).show()
    }
}