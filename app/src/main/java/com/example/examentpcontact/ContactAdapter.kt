
package com.example.examentpcontact

import android.graphics.Color
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.examentpcontact.data.Contact
import com.example.examentpcontact.databinding.ItemContactBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private object ContactDiff : DiffUtil.ItemCallback<Contact>() {
    override fun areItemsTheSame(a: Contact, b: Contact) = a.id == b.id
    override fun areContentsTheSame(a: Contact, b: Contact) = a == b
}

class ContactAdapter(
    private val onClick: (Contact) -> Unit,
    private val onAppel: (Contact) -> Unit,
    private val onFavori: (Contact) -> Unit,
    private val onSupprimer: (Contact) -> Unit
) : ListAdapter<Contact, ContactAdapter.VH>(ContactDiff) {

    private var tailleSp = 10f
    private var couleur = Color.BLACK
    private var appelActif = false

    private val format =
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    fun appliquerPreferences(taille: Float, couleur: Int, appelActif: Boolean) {
        this.tailleSp = taille
        this.couleur = couleur
        this.appelActif = appelActif
        notifyDataSetChanged()
    }

    class VH(val b: ItemContactBinding) :
        RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(
            ItemContactBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = getItem(position)

        with(holder.b) {

            tvNom.text = "${c.prenom} ${c.nom}"
            tvNom.setTextSize(TypedValue.COMPLEX_UNIT_SP, tailleSp)
            tvNom.setTextColor(couleur)

            tvTel.text = c.telephone
            tvTel.setTextSize(TypedValue.COMPLEX_UNIT_SP, tailleSp)

            imgPhoto.afficherPhoto(c.photoUri)

            if (c.dernierAppel > 0) {
                tvInfo.text =
                    "Dernier appel : ${format.format(Date(c.dernierAppel))}"
                tvInfo.visibility = View.VISIBLE
            } else {
                tvInfo.visibility = View.GONE
            }

            // Bouton Favoris
            btnFavori.setImageResource(
                if (c.favori) R.drawable.ic_star
                else R.drawable.ic_star_border
            )

            btnFavori.setOnClickListener {
                onFavori(c)
            }

            // Bouton Appeler
            zoneAppel.visibility =
                if (appelActif) View.VISIBLE else View.GONE

            btnAppeler.setOnClickListener {
                if (c.telephone.isNotBlank()) {
                    onAppel(c)
                }
            }

            // Bouton Supprimer
            btnSupprimer.setOnClickListener {
                onSupprimer(c)
            }

            // Clic sur le bloc contact pour modifier
            root.setOnClickListener {
                onClick(c)
            }
        }
    }
}
