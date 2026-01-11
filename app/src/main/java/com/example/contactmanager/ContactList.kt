package com.example.contactmanager

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ContactList : AppCompatActivity() {

    private lateinit var createConButton: Button
    private lateinit var backCMTV: TextView

    private var resultHelper = Permission(this)


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_contact_list)
        createConButton = findViewById<Button>(R.id.create_contact)
        backCMTV = findViewById<TextView>(R.id.con_manager_bTV)
        resultHelper.checkAndRequestContactsPermission{
            readContacts()
        }
        createConButton.setOnClickListener {
            val intent = Intent(this, AddContact::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            readContacts()
        } else {
            resultHelper.checkAndRequestContactsPermission {
                readContacts()
            }
        }
    }

    fun readContacts() {
        createConButton.visibility = View.VISIBLE
        backCMTV.visibility = View.INVISIBLE
        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 1)
        lifecycleScope.launch {
            val contactData = fetchContacts()
            recyclerView.adapter = ContactListAdapter(contactData) { clickedContact ->
                val intent = Intent(this@ContactList, OpenContact::class.java).apply {
                    putExtra("CONTACT_NAME", clickedContact.name)
                    putExtra("CONTACT_NUMBER", clickedContact.conNumber)
                    putExtra("CONTACT_IMAGE", clickedContact.conImage)
                }
                startActivity(intent)
            }
        }
    }

    suspend fun fetchContacts(): List<Contact> = withContext(Dispatchers.IO) {
        val contactList = mutableListOf<Contact>()
        val resolver = contentResolver

        // Projection defines which columns to retrieve
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI
        )
        val cursor = resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} COLLATE NOCASE ASC"
        )

        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex =
                it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER)
            val imageIndex =
                it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_THUMBNAIL_URI)

            while (it.moveToNext()) {
                val name = it.getString(nameIndex) ?: "Unknown"
                val number = it.getString(numberIndex) ?: ""
                val image = it.getString(imageIndex)

                contactList.add(Contact(name, number, image))
            }
        }
        contactList
    }

}