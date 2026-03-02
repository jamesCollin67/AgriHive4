package com.example.agrihive.hivestreams

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.agrihive.R
import com.example.agrihive.editprofile.EditProfileActivity

class HiveStreamsActivity : AppCompatActivity() {

    private lateinit var header: LinearLayout
    private lateinit var btnBack: ImageView
    private lateinit var bottomInfo: LinearLayout
    private lateinit var btnEdit: ImageView
    private lateinit var tvApiaryTitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_hive)

        // Correct types based on XML
        header = findViewById(R.id.header)           // LinearLayout
        btnBack = findViewById(R.id.btnBack)         // ImageView
        bottomInfo = findViewById(R.id.bottom_info)  // LinearLayout
        btnEdit = findViewById(R.id.btnEdit)         // ImageView
        tvApiaryTitle = findViewById(R.id.tvApiaryTitle) // TextView

        // Get apiary name from intent and set the title
        val apiaryName = intent.getStringExtra("APIARY_NAME")
        if (!apiaryName.isNullOrEmpty()) {
            tvApiaryTitle.text = apiaryName
        }

        btnBack.setOnClickListener {
            finish()
        }

        // Setup edit button dropdown menu
        btnEdit.setOnClickListener { view ->
            showEditDropdownMenu(view)
        }
    }

    private fun showEditDropdownMenu(view: android.view.View) {
        val popup = PopupMenu(this, view)
        popup.menuInflater.inflate(R.menu.menu_edit_options, popup.menu)
        
        popup.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.action_edit -> {
                    // Navigate to Edit Profile
                    Toast.makeText(this, "Edit functionality", Toast.LENGTH_SHORT).show()
                    // TODO: Navigate to edit screen if needed
                    true
                }
                R.id.action_send_report -> {
                    // Navigate to Send Report
                    val intent = Intent(this, SendReportActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
        
        popup.show()
    }
}
