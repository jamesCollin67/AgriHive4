package com.example.agrihive.hivestreams

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.agrihive.R
import com.example.agrihive.dashboard.DashboardActivity

class HiveStreamsActivity : AppCompatActivity() {

    private lateinit var header: LinearLayout
    private lateinit var btnBack: ImageView
    private lateinit var bottomInfo: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_hive)

        // Correct types based on XML
        header = findViewById(R.id.header)           // LinearLayout
        btnBack = findViewById(R.id.btnBack)         // ImageView
        bottomInfo = findViewById(R.id.bottom_info)  // LinearLayout

        btnBack.setOnClickListener {
            val intent = Intent(this, DashboardActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
