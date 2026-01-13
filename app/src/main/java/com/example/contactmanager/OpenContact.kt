package com.example.contactmanager

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class OpenContact : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_open_contact)
        val conName = findViewById<TextView>(R.id.tvName)
        val conNumber = findViewById<TextView>(R.id.tvNumber)
        val conImage = findViewById<ImageView>(R.id.ivContactImage)
        val name = intent.getStringExtra("CONTACT_NAME")
        val contact = intent.getStringExtra("CONTACT_NUMBER")
        val image = intent.getStringExtra("CONTACT_IMAGE")
        conName.text = name
        conNumber.text = contact

        if(image != null){
            conImage.setImageURI(image.toUri())
        }else {
            conImage.setImageResource(R.drawable.ic_person_background)
        }

        val editButton = findViewById<Button>(R.id.btEdit)
        editButton.setOnClickListener {
            val intent1 = Intent(this, UpdateContact::class.java).apply{
                putExtra("CONTACT_NAME", name)
                putExtra("CONTACT_NUMBER", contact)
                putExtra("CONTACT_IMAGE", image)
            }
            startActivity(intent1)
          //  finish()

        }

    }
}