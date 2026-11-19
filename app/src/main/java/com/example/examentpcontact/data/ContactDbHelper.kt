package com.example.examentpcontact.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.examentpcontact.data.ContactContract as C

class ContactDbHelper private constructor(context: Context) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE ${C.TABLE} (
                ${C.ID} INTEGER PRIMARY KEY AUTOINCREMENT,
                ${C.NOM} TEXT NOT NULL,
                ${C.PRENOM} TEXT NOT NULL,
                ${C.TELEPHONE} TEXT NOT NULL,
                ${C.EMAIL} TEXT,
                ${C.PHOTO_URI} TEXT,
                ${C.LATITUDE} REAL,
                ${C.LONGITUDE} REAL,
                ${C.FAVORI} INTEGER NOT NULL DEFAULT 0,
                ${C.DERNIER_APPEL} INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
        insererDemo(db)
    }

    // Migration : on ajoute les 2 colonnes sans perdre les contacts existants
    override fun onUpgrade(db: SQLiteDatabase, ancienne: Int, nouvelle: Int) {
        if (ancienne < 2) {
            db.execSQL("ALTER TABLE ${C.TABLE} ADD COLUMN ${C.FAVORI} INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE ${C.TABLE} ADD COLUMN ${C.DERNIER_APPEL} INTEGER NOT NULL DEFAULT 0")
        }
    }

    private fun insererDemo(db: SQLiteDatabase) {
        val demo = listOf(
            Contact(nom = "Ben Ali", prenom = "Ahmed", telephone = "20 123 456",
                email = "ahmed@mail.com", latitude = 36.8065, longitude = 10.1815),
            Contact(nom = "Trabelsi", prenom = "Sarra", telephone = "22 456 789",
                email = "sarra@mail.com", latitude = 34.7406, longitude = 10.7603),
            Contact(nom = "O'Neil", prenom = "Karim", telephone = "98 765 432",
                email = "karim@mail.com", latitude = 35.8256, longitude = 10.6084),
            Contact(nom = "Gharbi", prenom = "Mariem", telephone = "55 111 222"),        // sans GPS
            Contact(nom = "L'Ariana", prenom = "Youssef", telephone = "50 333 444") // sans GPS
        )
        for (c in demo) db.insert(C.TABLE, null, c.versValeurs())
    }

    companion object {
        const val DATABASE_NAME = "contacts.db"
        const val DATABASE_VERSION = 2   // était 1 : on l'incrémente pour déclencher onUpgrade

        @Volatile private var instance: ContactDbHelper? = null

        // Singleton construit avec applicationContext
        fun getInstance(context: Context): ContactDbHelper =
            instance ?: synchronized(this) {
                instance ?: ContactDbHelper(context.applicationContext).also { instance = it }
            }
    }
}