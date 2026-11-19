package com.example.examentpcontact.data

import android.content.ContentValues
import com.example.examentpcontact.data.ContactContract as C

// Pour insert/update : on n'écrit PAS favori ni dernier_appel,
// sinon modifier un contact remettrait son étoile à zéro.
fun Contact.versValeurs() = ContentValues().apply {
    put(C.NOM, nom)
    put(C.PRENOM, prenom)
    put(C.TELEPHONE, telephone)
    put(C.EMAIL, email)
    put(C.PHOTO_URI, photoUri)
    put(C.LATITUDE, latitude)
    put(C.LONGITUDE, longitude)
}

class ContactDao(private val helper: ContactDbHelper) {

    private val triNom = "${C.NOM} COLLATE NOCASE, ${C.PRENOM} COLLATE NOCASE"

    fun insert(contact: Contact): Long =
        helper.writableDatabase.insert(C.TABLE, null, contact.versValeurs())

    fun update(contact: Contact): Int =
        helper.writableDatabase.update(
            C.TABLE, contact.versValeurs(), "${C.ID} = ?", arrayOf(contact.id.toString())
        )

    fun delete(id: Long): Int =
        helper.writableDatabase.delete(C.TABLE, "${C.ID} = ?", arrayOf(id.toString()))

    fun getAll(): List<Contact> = liste("", TOUS)

    fun getById(id: Long): Contact? =
        requete("${C.ID} = ?", arrayOf(id.toString()), triNom).firstOrNull()

    fun search(texte: String): List<Contact> = liste(texte, TOUS)

    // Liste selon l'onglet (tous / favoris / récents) et le texte de recherche
    fun liste(texte: String, onglet: Int): List<Contact> {
        val conditions = mutableListOf<String>()
        val args = mutableListOf<String>()

        if (texte.isNotBlank()) {
            conditions.add("(${C.NOM} LIKE ? OR ${C.PRENOM} LIKE ? OR ${C.TELEPHONE} LIKE ?)")
            repeat(3) { args.add("%$texte%") }
        }
        if (onglet == FAVORIS) conditions.add("${C.FAVORI} = 1")
        if (onglet == RECENTS) conditions.add("${C.DERNIER_APPEL} > 0")

        val selection = if (conditions.isEmpty()) null else conditions.joinToString(" AND ")
        val tri = if (onglet == RECENTS) "${C.DERNIER_APPEL} DESC" else triNom
        return requete(selection, if (args.isEmpty()) null else args.toTypedArray(), tri)
    }

    // Met ou retire l'étoile
    fun setFavori(id: Long, favori: Boolean): Int {
        val valeurs = ContentValues().apply { put(C.FAVORI, if (favori) 1 else 0) }
        return helper.writableDatabase.update(
            C.TABLE, valeurs, "${C.ID} = ?", arrayOf(id.toString())
        )
    }

    // Enregistre l'heure de l'appel
    fun marquerAppel(id: Long): Int {
        val valeurs = ContentValues().apply { put(C.DERNIER_APPEL, System.currentTimeMillis()) }
        return helper.writableDatabase.update(
            C.TABLE, valeurs, "${C.ID} = ?", arrayOf(id.toString())
        )
    }

    private fun requete(selection: String?, args: Array<String>?, tri: String): List<Contact> {
        val liste = mutableListOf<Contact>()
        helper.readableDatabase.query(C.TABLE, null, selection, args, null, null, tri).use { c ->
            val iId = c.getColumnIndexOrThrow(C.ID)
            val iNom = c.getColumnIndexOrThrow(C.NOM)
            val iPrenom = c.getColumnIndexOrThrow(C.PRENOM)
            val iTel = c.getColumnIndexOrThrow(C.TELEPHONE)
            val iEmail = c.getColumnIndexOrThrow(C.EMAIL)
            val iPhoto = c.getColumnIndexOrThrow(C.PHOTO_URI)
            val iLat = c.getColumnIndexOrThrow(C.LATITUDE)
            val iLon = c.getColumnIndexOrThrow(C.LONGITUDE)
            val iFav = c.getColumnIndexOrThrow(C.FAVORI)
            val iAppel = c.getColumnIndexOrThrow(C.DERNIER_APPEL)
            while (c.moveToNext()) {
                liste.add(
                    Contact(
                        id = c.getLong(iId),
                        nom = c.getString(iNom),
                        prenom = c.getString(iPrenom),
                        telephone = c.getString(iTel),
                        email = c.getString(iEmail),
                        photoUri = c.getString(iPhoto),
                        latitude = if (c.isNull(iLat)) null else c.getDouble(iLat),
                        longitude = if (c.isNull(iLon)) null else c.getDouble(iLon),
                        favori = c.getInt(iFav) == 1,
                        dernierAppel = c.getLong(iAppel)
                    )
                )
            }
        }
        return liste
    }

    companion object {
        const val TOUS = 0
        const val FAVORIS = 1
        const val RECENTS = 2
    }
}