package com.example.agrihive.hivestreams

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.example.agrihive.R

class ReportSentActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_report_sent)

        btnBack = findViewById(R.id.btnBack)

        btnBack.setOnClickListener {
            // Go back to View Hive (HiveStreamsActivity)
            finish()
        }
    }
}
