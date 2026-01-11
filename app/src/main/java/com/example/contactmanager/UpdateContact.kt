package com.example.contactmanager

import android.content.ContentProviderOperation
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class UpdateContact : AppCompatActivity() {
    private var selectedBitmap: Bitmap? = null

    private lateinit var conImage: ImageView

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Convert Uri to Bitmap and display it
            val inputStream = contentResolver.openInputStream(it)
            selectedBitmap = BitmapFactory.decodeStream(inputStream)
            conImage.setImageBitmap(selectedBitmap)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_update_contact)

        val conName = findViewById<EditText>(R.id.evName)
        val conNumber = findViewById<EditText>(R.id.evCon_num)
        conImage = findViewById<ImageView>(R.id.conImage)
        val saveButton = findViewById<Button>(R.id.btSave)
        saveButton.isEnabled = false
        var contactId: Long? = null

        val name = intent.getStringExtra("CONTACT_NAME")
        val contact = intent.getStringExtra("CONTACT_NUMBER")
        val image = intent.getStringExtra("CONTACT_IMAGE")

        if (image != null) {
            conImage.setImageURI(image.toUri())
        } else {
            conImage.setImageResource(R.drawable.ic_person_background)
        }

        conImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        conName.setText(name)
        conNumber.setText(contact)

        lifecycleScope.launch {
            val id = getContactIdByName(name ?: "")
            if (id != null) {
                contactId = id
                saveButton.isEnabled = true
            } else {
                // Handle case where contact isn't found
                Toast.makeText(this@UpdateContact, "Contact not found", Toast.LENGTH_SHORT).show()
            }
        }


        saveButton.setOnClickListener {
            val currentId = contactId
            val name = conName.text.toString()
            val number = conNumber.text.toString()
            if (!name.isBlank() && !number.isBlank() && currentId != null) {
                lifecycleScope.launch {
                    val smallBitmap = selectedBitmap?.let {
                        Bitmap.createScaledBitmap(it, 300, 300, true)
                    }
                    saveContact(
                        conName.getText().toString(),
                        conNumber.getText().toString(),
                        smallBitmap,
                        currentId
                    )
                    //Toast.makeText(this@UpdateContact, "Contact Updated", Toast.LENGTH_SHORT).show()
                   // finish()
                }
            }
        }

    }

    suspend fun saveContact(name: String, contact: String, image: Bitmap?, contactId: Long) =
        withContext(Dispatchers.IO) {

            val operations = arrayListOf<ContentProviderOperation>()

            if (contactId <= 0) return@withContext

            name.let {
                operations.add(
                    ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                        .withSelection(
                            "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                            arrayOf(
                                contactId.toString(),
                                ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                            )
                        )
                        .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, it)
                        .build()
                )
            }

            contact.let {
                operations.add(
                    ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                        .withSelection(
                            "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ? AND ${ContactsContract.CommonDataKinds.Phone.TYPE} = ?",
                            arrayOf(
                                contactId.toString(),
                                ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE,
                                ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE.toString()
                            )
                        )
                        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, it)
                        .build()
                )
            }
            image?.let {
                val imageBytes = bitmapToByteArray(it)

                // Check if photo already exists for this contact
                val photoExists = doesPhotoExist(contactId)

                val op = if (photoExists) {
                    ContentProviderOperation.newUpdate(ContactsContract.Data.CONTENT_URI)
                        .withSelection(
                            "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?",
                            arrayOf(
                                contactId.toString(),
                                ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE
                            )
                        )
                } else {
                    // Get RAW_CONTACT_ID because Data.CONTENT_URI inserts need it
                    val rawId = getRawContactId(contactId)
                    ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                        .withValue(ContactsContract.Data.RAW_CONTACT_ID, rawId)
                        .withValue(
                            ContactsContract.Data.MIMETYPE,
                            ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE
                        )
                }

                operations.add(
                    op.withValue(
                        ContactsContract.CommonDataKinds.Photo.PHOTO,
                        imageBytes
                    ).build()
                )
            }

            try {
                contentResolver.applyBatch(ContactsContract.AUTHORITY, operations)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }


    private fun doesPhotoExist(contactId: Long): Boolean {
        val selection =
            "${ContactsContract.Data.CONTACT_ID} = ? AND ${ContactsContract.Data.MIMETYPE} = ?"
        val args =
            arrayOf(contactId.toString(), ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
        contentResolver.query(
            ContactsContract.Data.CONTENT_URI,
            arrayOf(ContactsContract.Data._ID),
            selection,
            args,
            null
        )?.use {
            return it.count > 0
        }
        return false
    }

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        // Compress to JPEG to stay under the 1MB transaction limit
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream)
        return stream.toByteArray()
    }

    private fun getRawContactId(contactId: Long): Long {
        val projection = arrayOf(ContactsContract.RawContacts._ID)
        val selection = "${ContactsContract.RawContacts.CONTACT_ID} = ?"
        val selectionArgs = arrayOf(contactId.toString())

        contentResolver.query(
            ContactsContract.RawContacts.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getLong(cursor.getColumnIndexOrThrow(ContactsContract.RawContacts._ID))
            }
        }
        return -1L // Return a fallback value if not found
    }


    suspend fun getContactIdByName(displayName: String): Long? = withContext(Dispatchers.IO){
        var contactId: Long? = null
        val selection = "${ContactsContract.Contacts.DISPLAY_NAME} = ?"
        val selectionArgs = arrayOf(displayName)

        val projection = arrayOf(ContactsContract.Contacts._ID)

        contentResolver.query(
            ContactsContract.Contacts.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                contactId =
                    cursor.getLong(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
            }
        }
        contactId
    }


}