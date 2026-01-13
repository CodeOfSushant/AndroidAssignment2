package com.example.contactmanager

import android.content.ContentProviderOperation
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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.hbb20.CountryCodePicker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class AddContact : AppCompatActivity() {

    private var selectedBitmap: Bitmap? = null
    private lateinit var imageView: ImageView
    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Convert Uri to Bitmap and display it
            val inputStream = contentResolver.openInputStream(it)
            selectedBitmap = BitmapFactory.decodeStream(inputStream)
            imageView.setImageBitmap(selectedBitmap)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_add_contact)
        val saveButton = findViewById<Button>(R.id.saveButton)
        imageView = findViewById<ImageView>(R.id.saveImage)

        imageView.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
        saveButton.setOnClickListener {
            val name = findViewById<EditText>(R.id.etFirstName).text.toString()
            val etNumber = findViewById<EditText>(R.id.etPhoneNo)
            val ccp = findViewById<CountryCodePicker>(R.id.ccp)
            ccp.registerCarrierNumberEditText(etNumber)
            val conNumber = etNumber.text.toString()
            val fullNumber = ccp.fullNumberWithPlus

            if (!ccp.isValidFullNumber) {
                etNumber.error = "Invalid phone number"
            }else {
                if (!name.isBlank() && !conNumber.isBlank()) {
                    lifecycleScope.launch {
                        val success = saveContact(name, fullNumber, selectedBitmap)
                        if (success) {
                            Toast.makeText(this@AddContact, "Contact Saved!", Toast.LENGTH_SHORT)
                                .show()
                            finish() // Go back to the list
                        } else {
                            Toast.makeText(
                                this@AddContact,
                                "Error saving contact",
                                Toast.LENGTH_SHORT
                            )
                                .show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Please enter name and phone", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }
        }
    }

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        // Compress to JPEG to stay under the 1MB transaction limit
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        return stream.toByteArray()
    }

    suspend fun saveContact(name: String, conNumber: String, photo: Bitmap?): Boolean =
        withContext(Dispatchers.IO) {
            val ops = arrayListOf<ContentProviderOperation>()

            // 1. Create a new Raw Contact
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .build()
            )

            // 2. Add the Display Name
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE
                    )
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                    .build()
            )

            // 3. Add the Phone Number
            ops.add(
                ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(
                        ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE
                    )
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, conNumber)
                    .withValue(
                        ContactsContract.CommonDataKinds.Phone.TYPE,
                        ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE
                    )
                    .build()
            )

            // Add image
            photo?.let {
                val imageBytes = bitmapToByteArray(it)
                ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Photo.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Photo.PHOTO, imageBytes)
                    .build())
            }

            try {
                contentResolver.applyBatch(ContactsContract.AUTHORITY, ops)
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
}
