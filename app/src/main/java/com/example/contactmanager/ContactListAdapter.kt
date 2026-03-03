package com.example.contactmanager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.core.net.toUri

class ContactListAdapter(val contacts: List<Contact>, private val onItemClick: (Contact) -> Unit) :
    RecyclerView.Adapter<ContactListAdapter.ContactViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ContactViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.contact_cardview, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contacts[position]
        holder.tvName.text = contact.name
        holder.tvNumber.text = contact.conNumber

        holder.itemView.setOnClickListener {
            onItemClick(contact) // 2. Trigger the callback
        }

        // Attempt to load photo (use the contact's thumbnail URI)
        if (contact.conImage != null) {
            holder.ivContactImage.visibility = View.VISIBLE
            holder.tvLetterPlaceHolder.visibility = View.GONE
            holder.ivContactImage.setImageURI(contact.conImage.toUri())
        } else {
            holder.ivContactImage.visibility = View.GONE
            holder.tvLetterPlaceHolder.visibility = View.VISIBLE

            val firstLetter = if (contact.name.isNotEmpty()) {
                contact.name[0].uppercaseChar().toString()
            } else {
                "#"
            }
            holder.tvLetterPlaceHolder.text = firstLetter
        }
    }

    override fun getItemCount(): Int {
        return contacts.size
    }

    class ContactViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvName)
        val tvNumber: TextView = view.findViewById(R.id.tvNumber)

        val ivContactImage: ImageView = view.findViewById(R.id.ivContactImage)

        val tvLetterPlaceHolder: TextView = view.findViewById(R.id.tvLetterPlaceholder)

    }

}