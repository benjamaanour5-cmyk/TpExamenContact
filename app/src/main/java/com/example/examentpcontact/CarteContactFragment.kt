package com.example.examentpcontact

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.launch

class CarteContactFragment : Fragment(R.layout.fragment_carte), OnMapReadyCallback {

    private val vm: ContactViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val carte = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        carte.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        val id = requireArguments().getLong(DetailContactFragment.ARG_ID)
        viewLifecycleOwner.lifecycleScope.launch {
            val c = vm.getContact(id)
            val lat = c?.latitude
            val lon = c?.longitude
            when {
                c == null -> afficherMessage("Contact introuvable")
                lat == null || lon == null ->
                    afficherMessage("Aucune position enregistrée pour ce contact")
                else -> {
                    val position = LatLng(lat, lon)
                    map.addMarker(MarkerOptions().position(position).title("${c.prenom} ${c.nom}"))
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(position, 14f))
                }
            }
        }
    }

    private fun afficherMessage(texte: String) {
        view?.findViewById<TextView>(R.id.tvMessage)?.apply {
            text = texte
            visibility = View.VISIBLE
        }
    }

    companion object {
        fun nouveau(id: Long) = CarteContactFragment().apply {
            arguments = bundleOf(DetailContactFragment.ARG_ID to id)
        }
    }
}