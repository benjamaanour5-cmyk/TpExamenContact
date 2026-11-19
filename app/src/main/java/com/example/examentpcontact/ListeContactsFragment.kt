package com.example.examentpcontact

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.examentpcontact.data.Contact
import com.example.examentpcontact.databinding.FragmentListeContactsBinding

class ListeContactsFragment : Fragment(R.layout.fragment_liste_contacts) {

    private val vm: ContactViewModel by activityViewModels()
    private var _b: FragmentListeContactsBinding? = null
    private val b get() = _b!!
    private lateinit var prefs: PreferencesManager
    private lateinit var adapter: ContactAdapter

    // Position dans cette liste = numéro d'onglet du ViewModel (0, 1, 2)
    private val ongletsMenu = listOf(R.id.nav_contacts, R.id.nav_favoris, R.id.nav_recents)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _b = FragmentListeContactsBinding.bind(view)
        prefs = PreferencesManager(requireContext())

        adapter = ContactAdapter(
            onClick = { naviguerVers(DetailContactFragment.nouveau(it.id)) },

            onAppel = { c ->
                appeler(requireContext(), c.telephone)
                vm.marquerAppel(c.id)
            },

            onFavori = { vm.basculerFavori(it) },
            onSupprimer = { confirmerSuppression(it) }
        )
        b.recycler.layoutManager = LinearLayoutManager(requireContext())
        b.recycler.adapter = adapter

        vm.contacts.observe(viewLifecycleOwner) { liste ->
            adapter.submitList(liste)
            b.tvVide.text = when (vm.onglet) {
                1 -> "Aucun favori"
                2 -> "Aucun appel récent"
                else -> "Aucun contact"
            }
            b.tvVide.isVisible = liste.isEmpty()
        }

        // Menu du bas : on sélectionne d'abord l'onglet mémorisé, puis on écoute les clics
        b.bottomNav.selectedItemId = ongletsMenu[vm.onglet]
        b.bottomNav.setOnItemSelectedListener { item ->
            vm.changerOnglet(ongletsMenu.indexOf(item.itemId))
            true
        }

        b.etFiltre.setText(vm.filtre)
        b.etFiltre.doAfterTextChanged { vm.filtrer(it.toString()) }

        b.btnAjouter.setOnClickListener {
            naviguerVers(DetailContactFragment.nouveau(0L))
        }    }

    // Suppression depuis la liste, avec confirmation
    private fun confirmerSuppression(c: Contact) {
        AlertDialog.Builder(requireContext())
            .setTitle("Supprimer")
            .setMessage("Supprimer ${c.prenom} ${c.nom} ?")
            .setPositiveButton("Supprimer") { _, _ ->
                vm.supprimer(c.id) { erreur -> toast(erreur ?: "Contact supprimé") }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        adapter.appliquerPreferences(prefs.tailleSp(), prefs.couleur(), prefs.appelActif())
        vm.charger()
    }

    override fun onDestroyView() {
        _b = null
        super.onDestroyView()
    }
}