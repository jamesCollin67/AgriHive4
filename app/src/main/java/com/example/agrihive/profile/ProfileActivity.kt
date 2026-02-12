package com.example.agrihive.profile

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.agrihive.R
import com.example.agrihive.settings.SettingsActivity
import com.example.agrihive.editprofile.EditProfileActivity
import com.example.agrihive.dashboard.DashboardActivity

class ProfileActivity : AppCompatActivity() {

    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile_page)


        val userName = findViewById<TextView>(R.id.userName)
        val btnEdit = findViewById<Button>(R.id.btnEditProfile)
        val backBtn = findViewById<ImageView>(R.id.btnBack)

        // AUTO REFLECT DATA
        viewModel.user.observe(this){

            userName.text = "${it.firstName} ${it.lastName}"
        }


        btnEdit.setOnClickListener {
            viewModel.editClicked()
        }

        backBtn.setOnClickListener {
            viewModel.backClicked()
        }


        observeNavigation()
    }



    private fun observeNavigation(){

        viewModel.goEdit.observe(this){

            if(it == true){

                startActivity(Intent(this, EditProfileActivity::class.java))
                viewModel.doneNav()
            }
        }

        viewModel.goDashboard.observe(this){

            if(it == true){

                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
                viewModel.doneNav()
            }
        }

        viewModel.goSettings.observe(this){

            if(it == true){

                startActivity(Intent(this, SettingsActivity::class.java))
                viewModel.doneNav()
            }
        }
    }
}