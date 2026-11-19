package com.example.examentpcontact

import android.content.ActivityNotFoundException
import android.os.Bundle
import android.util.TypedValue
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.example.examentpcontact.data.Contact
import com.example.examentpcontact.databinding.FragmentDetailContactBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DetailContactFragment : Fragment(R.layout.fragment_detail_contact) {

    private val vm: ContactViewModel by activityViewModels()
    private var _b: FragmentDetailContactBinding? = null
    private val b get() = _b!!

    private var idContact = 0L          // 0 = mode création
    private var photoPath: String? = null
    private var dejaCharge = false      // évite d'écraser la saisie après rotation

    // ----- Galerie et caméra -----
    private val galerie = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewLifecycleOwner.lifecycleScope.launch {
            val chemin = withContext(Dispatchers.IO) { copierUri(requireContext(), uri) }
            changerPhoto(chemin)
        }
    }

    private val camera = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) viewLifecycleOwner.lifecycleScope.launch {
            val chemin = withContext(Dispatchers.IO) { sauverBitmap(requireContext(), bitmap) }
            changerPhoto(chemin)
        }
        // bitmap == null : l'utilisateur a annulé, on ne fait rien
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        idContact = requireArguments().getLong(ARG_ID)
        photoPath = savedInstanceState?.getString("photo")
        dejaCharge = savedInstanceState?.getBoolean("charge") ?: false
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _b = FragmentDetailContactBinding.bind(view)

        // Préférences : taille (nom, prénom, téléphone) et couleur (nom, prénom)
        val prefs = PreferencesManager(requireContext())
        listOf(b.etNom, b.etPrenom, b.etTelephone).forEach {
            it.setTextSize(TypedValue.COMPLEX_UNIT_SP, prefs.tailleSp())
        }
        b.etNom.setTextColor(prefs.couleur())
        b.etPrenom.setTextColor(prefs.couleur())

        b.imgPhoto.afficherPhoto(photoPath)
        b.btnSupprimer.isVisible = idContact != 0L
        if (idContact != 0L && !dejaCharge) chargerContact()

        b.btnGalerie.setOnClickListener { galerie.launch("image/*") }
        b.btnCamera.setOnClickListener {
            try {
                camera.launch(null)
            } catch (e: ActivityNotFoundException) {
                toast("Aucune application caméra")
            }
        }
        b.btnAppeler.setOnClickListener {
            val tel = b.etTelephone.text.toString().trim()
            if (idContact != 0L && tel.isNotBlank()) vm.marquerAppel(idContact)
            appeler(requireContext(), tel)
        }
        b.btnEmail.setOnClickListener {
            envoyerEmail(requireContext(), b.etEmail.text.toString().trim())
        }
        b.btnCarte.setOnClickListener {
            if (idContact == 0L) toast("Enregistrez d'abord le contact")
            else naviguerVers(CarteContactFragment.nouveau(idContact))
        }
        b.btnEnregistrer.setOnClickListener { enregistrer() }
        b.btnAnnuler.setOnClickListener { parentFragmentManager.popBackStack() }
        b.btnSupprimer.setOnClickListener { confirmerSuppression() }
    }

    private fun chargerContact() {
        viewLifecycleOwner.lifecycleScope.launch {
            val c = vm.getContact(idContact)
            if (_b == null) return@launch
            if (c == null) {
                toast("Contact introuvable")
                parentFragmentManager.popBackStack()
                return@launch
            }
            b.etNom.setText(c.nom)
            b.etPrenom.setText(c.prenom)
            b.etTelephone.setText(c.telephone)
            b.etEmail.setText(c.email ?: "")
            b.etLatitude.setText(c.latitude?.toString() ?: "")
            b.etLongitude.setText(c.longitude?.toString() ?: "")
            photoPath = c.photoUri
            b.imgPhoto.afficherPhoto(photoPath)
            dejaCharge = true
        }
    }

    private fun changerPhoto(chemin: String?) {
        if (_b == null) return
        if (chemin == null) {
            toast("Impossible de lire l'image")
            return
        }
        photoPath = chemin
        b.imgPhoto.afficherPhoto(chemin)
    }

    private fun enregistrer() {
        val nom = b.etNom.text.toString().trim()
        val prenom = b.etPrenom.text.toString().trim()
        val tel = b.etTelephone.text.toString().trim()
        val email = b.etEmail.text.toString().trim()

        // Validation : rien n'est écrit en base si un champ obligatoire manque
        if (nom.isEmpty()) { b.etNom.error = "Nom obligatoire"; return }
        if (prenom.isEmpty()) { b.etPrenom.error = "Prénom obligatoire"; return }
        if (tel.isEmpty()) { b.etTelephone.error = "Téléphone obligatoire"; return }

        // Coordonnées : toutes les deux vides, ou toutes les deux valides
        val latTxt = b.etLatitude.text.toString().trim().replace(',', '.')
        val lonTxt = b.etLongitude.text.toString().trim().replace(',', '.')
        var lat: Double? = null
        var lon: Double? = null
        if (latTxt.isNotEmpty() || lonTxt.isNotEmpty()) {
            lat = latTxt.toDoubleOrNull()
            lon = lonTxt.toDoubleOrNull()
            if (lat == null || lon == null || lat !in -90.0..90.0 || lon !in -180.0..180.0) {
                b.etLatitude.error = "Coordonnées invalides (lat -90..90, lon -180..180)"
                return
            }
        }

        val contact = Contact(
            id = idContact, nom = nom, prenom = prenom, telephone = tel,
            email = email.ifEmpty { null }, photoUri = photoPath,
            latitude = lat, longitude = lon
        )
        vm.enregistrer(contact) { erreur ->
            if (!isAdded) return@enregistrer
            if (erreur == null) {
                toast("Contact enregistré")
                parentFragmentManager.popBackStack()
            } else {
                toast(erreur)
            }
        }
    }

    private fun confirmerSuppression() {
        AlertDialog.Builder(requireContext())
            .setTitle("Supprimer")
            .setMessage("Voulez-vous vraiment supprimer ce contact ?")
            .setPositiveButton("Supprimer") { _, _ ->
                vm.supprimer(idContact) { erreur ->
                    if (!isAdded) return@supprimer
                    if (erreur == null) {
                        toast("Contact supprimé")
                        parentFragmentManager.popBackStack()
                    } else {
                        toast(erreur)
                    }
                }
            }
            .setNegativeButton("Annuler", null)
            .show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("photo", photoPath)
        outState.putBoolean("charge", dejaCharge)
    }

    override fun onDestroyView() {
        _b = null
        super.onDestroyView()
    }

    companion object {
        const val ARG_ID = "id"
        fun nouveau(id: Long) = DetailContactFragment().apply {
            arguments = bundleOf(ARG_ID to id)
        }
    }
}