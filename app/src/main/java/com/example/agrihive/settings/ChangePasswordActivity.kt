package com.example.agrihive.settings

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.agrihive.R
import com.example.agrihive.dashboard.DashboardActivity
import com.example.agrihive.log.ActivityLogViewModel
import com.example.agrihive.utils.NetworkUtils
import com.google.firebase.auth.EmailAuthCredential
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var btnChangePassword: androidx.appcompat.widget.AppCompatButton
    private lateinit var etCurrentPassword: EditText
    private lateinit var etNewPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var ivToggleCurrentPassword: ImageView
    private lateinit var ivToggleNewPassword: ImageView
    private lateinit var ivToggleConfirmPassword: ImageView
    private lateinit var progressBar: ProgressBar

    private var isCurrentPasswordVisible = false
    private var isNewPasswordVisible = false
    private var isConfirmPasswordVisible = false

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val currentUser = firebaseAuth.currentUser
    private val activityLogViewModel: ActivityLogViewModel by lazy { ActivityLogViewModel.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        initViews()
        setupUI()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        btnChangePassword = findViewById(R.id.btnChangePassword)
        etCurrentPassword = findViewById(R.id.etCurrentPassword)
        etNewPassword = findViewById(R.id.etNewPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        ivToggleCurrentPassword = findViewById(R.id.ivToggleCurrentPassword)
        ivToggleNewPassword = findViewById(R.id.ivToggleNewPassword)
        ivToggleConfirmPassword = findViewById(R.id.ivToggleConfirmPassword)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupUI() {
        // Back button navigation
        btnBack.setOnClickListener {
            finish()
        }

        // Password visibility toggles
        ivToggleCurrentPassword.setOnClickListener {
            togglePasswordVisibility(etCurrentPassword, ivToggleCurrentPassword, isCurrentPasswordVisible)
            isCurrentPasswordVisible = !isCurrentPasswordVisible
        }

        ivToggleNewPassword.setOnClickListener {
            togglePasswordVisibility(etNewPassword, ivToggleNewPassword, isNewPasswordVisible)
            isNewPasswordVisible = !isNewPasswordVisible
        }

        ivToggleConfirmPassword.setOnClickListener {
            togglePasswordVisibility(etConfirmPassword, ivToggleConfirmPassword, isConfirmPasswordVisible)
            isConfirmPasswordVisible = !isConfirmPasswordVisible
        }

        // Change password button
        btnChangePassword.setOnClickListener {
            // Get the input values
            val currentPassword = etCurrentPassword.text.toString()
            val newPassword = etNewPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            // Validate inputs
            when {
                currentPassword.isEmpty() -> {
                    Toast.makeText(this, "Please enter your current password", Toast.LENGTH_SHORT).show()
                }
                newPassword.isEmpty() -> {
                    Toast.makeText(this, "Please enter your new password", Toast.LENGTH_SHORT).show()
                }
                confirmPassword.isEmpty() -> {
                    Toast.makeText(this, "Please confirm your new password", Toast.LENGTH_SHORT).show()
                }
                newPassword != confirmPassword -> {
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                }
                newPassword.length < 6 -> {
                    Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                }
                currentUser?.email == null -> {
                    Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    // Check internet connection first
                    if (!NetworkUtils.isNetworkAvailable(this)) {
                        Toast.makeText(this, "No internet connection. Please check your network and try again.", Toast.LENGTH_LONG).show()
                    } else {
                        // Re-authenticate and update password using Firebase
                        changePassword(currentPassword, newPassword)
                    }
                }
            }
        }
    }

    private fun changePassword(currentPassword: String, newPassword: String) {
        val user = firebaseAuth.currentUser
        val email = user?.email

        if (email == null) {
            Toast.makeText(this, "Unable to get user email", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading indicator
        progressBar.visibility = View.VISIBLE
        btnChangePassword.isEnabled = false
        btnChangePassword.text = "Changing..."
        // Disable input fields during loading
        etCurrentPassword.isEnabled = false
        etNewPassword.isEnabled = false
        etConfirmPassword.isEnabled = false

        // Re-authenticate user before changing password
        val credential = EmailAuthProvider.getCredential(email, currentPassword)

        user.reauthenticate(credential)
            .addOnCompleteListener { reauthTask ->
                if (reauthTask.isSuccessful) {
                    // Re-authentication successful, now update password
                    user.updatePassword(newPassword)
                        .addOnCompleteListener { updateTask ->
                            // Hide loading indicator
                            progressBar.visibility = View.GONE
                            btnChangePassword.isEnabled = true
                            btnChangePassword.text = "CHANGE PASSWORD"
                            // Re-enable input fields
                            etCurrentPassword.isEnabled = true
                            etNewPassword.isEnabled = true
                            etConfirmPassword.isEnabled = true
                            
                            if (updateTask.isSuccessful) {
                                Toast.makeText(this, "Password changed successfully", Toast.LENGTH_SHORT).show()
                                
                                // Log the password change activity
                                activityLogViewModel.logPasswordChanged()
                                
                                // Navigate back to settings
                                finish()
                            } else {
                                Toast.makeText(this, "Failed to update password: ${updateTask.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    // Hide loading indicator
                    progressBar.visibility = View.GONE
                    btnChangePassword.isEnabled = true
                    btnChangePassword.text = "CHANGE PASSWORD"
                    // Re-enable input fields
                    etCurrentPassword.isEnabled = true
                    etNewPassword.isEnabled = true
                    etConfirmPassword.isEnabled = true
                    
                    // Re-authentication failed
                    val errorMessage = when {
                        reauthTask.exception?.message?.contains("INVALID_CREDENTIALS") == true ->
                            "Current password is incorrect"
                        reauthTask.exception?.message?.contains("too many requests") == true ->
                            "Too many attempts. Please try again later"
                        else ->
                            "Failed to verify current password: ${reauthTask.exception?.message}"
                    }
                    Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun togglePasswordVisibility(editText: EditText, toggleIcon: ImageView, isVisible: Boolean) {
        if (isVisible) {
            // Hide password
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            toggleIcon.setImageResource(R.drawable.ic_eye_off)
        } else {
            // Show password
            editText.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            toggleIcon.setImageResource(R.drawable.ic_eye)
        }
        // Move cursor to end
        editText.setSelection(editText.text?.length ?: 0)
    }
}
