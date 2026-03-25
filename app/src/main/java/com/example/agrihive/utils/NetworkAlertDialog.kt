package com.example.agrihive.utils

import android.app.AlertDialog
import android.content.Context

object NetworkAlertDialog {
    fun show(
        context: Context,
        onTryAgain: (() -> Unit)? = null,
        onCancel: (() -> Unit)? = null
    ) {
        AlertDialog.Builder(context)
            .setTitle("No internet connection")
            .setMessage("Please check your network and try again.")
            .setPositiveButton("Try Again") { _, _ -> onTryAgain?.invoke() }
            .setNegativeButton("Cancel") { _, _ -> onCancel?.invoke() }
            .show()
    }
}
