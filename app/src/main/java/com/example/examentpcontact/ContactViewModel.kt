
package com.example.examentpcontact

import android.app.Application
import android.database.SQLException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.examentpcontact.data.Contact
import com.example.examentpcontact.data.ContactDao
import com.example.examentpcontact.data.ContactDbHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log
import android.widget.Toast
class ContactViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = ContactDao(ContactDbHelper.getInstance(app))

    private val _contacts = MutableLiveData<List<Contact>>()
    val contacts: LiveData<List<Contact>> = _contacts

    var filtre = ""
        private set

    var onglet = 0
        private set

    private var job: Job? = null

    // Charger les contacts selon l'onglet sélectionné
    fun charger() {
        job?.cancel()

        job = viewModelScope.launch {
            val liste = withContext(Dispatchers.IO) {
                val tous = if (filtre.isBlank()) {
                    dao.getAll()
                } else {
                    dao.search(filtre.trim())
                }

                when (onglet) {
                    1 -> tous.filter { it.favori }
                    2 -> tous.filter { it.dernierAppel > 0 }
                    else -> tous
                }
            }

            _contacts.value = liste
        }
    }

    // Changer d'onglet
    fun changerOnglet(position: Int) {
        if (position in 0..2) {
            onglet = position
            charger()
        }
    }

    // Filtrer les contacts
    fun filtrer(texte: String) {
        filtre = texte
        charger()
    }

    // Enregistre l'heure d'un appel (alimente l'onglet Récents)
    fun marquerAppel(id: Long) {
        viewModelScope.launch {
            val lignes = withContext(Dispatchers.IO) {
                try {
                    dao.marquerAppel(id)
                } catch (e: Exception) {
                    Log.e("Contacts", "Erreur appel", e)
                    -1
                }
            }
            Log.d("Contacts", "appel id=$id lignes modifiées=$lignes")
            if (lignes <= 0) {
                Toast.makeText(getApplication(), "Appel non enregistré", Toast.LENGTH_LONG).show()
            }
            charger()
        }
    }

    // Ajouter ou retirer un favori
    fun basculerFavori(c: Contact) {
        viewModelScope.launch {
            val lignes = withContext(Dispatchers.IO) {
                try {
                    dao.setFavori(c.id, !c.favori)
                } catch (e: Exception) {
                    Log.e("Contacts", "Erreur favori", e)
                    -1
                }
            }
            Log.d("Contacts", "favori id=${c.id} lignes modifiées=$lignes")
            if (lignes <= 0) {
                Toast.makeText(getApplication(), "Favori non enregistré", Toast.LENGTH_LONG).show()
            }
            charger()
        }
    }

    suspend fun getContact(id: Long): Contact? =
        withContext(Dispatchers.IO) {
            dao.getById(id)
        }

    // Enregistrer un contact
    fun enregistrer(contact: Contact, termine: (String?) -> Unit) {
        viewModelScope.launch {
            val erreur = withContext(Dispatchers.IO) {
                try {
                    if (contact.id == 0L) {
                        if (dao.insert(contact) == -1L)
                            "Insertion échouée"
                        else null
                    } else {
                        if (dao.update(contact) == 0)
                            "Contact introuvable"
                        else null
                    }
                } catch (e: SQLException) {
                    "Erreur de base de données"
                }
            }

            if (erreur == null) charger()
            termine(erreur)
        }
    }

    // Supprimer un contact
    fun supprimer(id: Long, termine: (String?) -> Unit) {
        viewModelScope.launch {
            val erreur = withContext(Dispatchers.IO) {
                try {
                    if (dao.delete(id) == 0)
                        "Contact introuvable"
                    else null
                } catch (e: SQLException) {
                    "Erreur de base de données"
                }
            }

            if (erreur == null) charger()
            termine(erreur)
        }
    }
}
