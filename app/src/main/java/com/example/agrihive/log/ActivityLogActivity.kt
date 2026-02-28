package com.example.agrihive.log

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.agrihive.R

class ActivityLogActivity : AppCompatActivity() {

    private val viewModel: ActivityLogViewModel by viewModels()
    private lateinit var adapter: ActivityLogAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)

        setupViews()
        observeViewModel()
    }

    private fun setupViews() {
        // Back button
        findViewById<View>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // RecyclerView setup
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerActivityLog)
        adapter = ActivityLogAdapter()

        recyclerView.apply {
            this.adapter = this@ActivityLogActivity.adapter
            layoutManager = LinearLayoutManager(this@ActivityLogActivity)
        }
    }

    private fun observeViewModel() {
        viewModel.activityLogs.observe(this) { logs ->
            adapter.submitList(logs)
        }
    }
}
