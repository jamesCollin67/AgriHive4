package com.example.agrihive.utils

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import android.widget.Button
import android.widget.TextView
import com.example.agrihive.R

/**
 * Custom dialog for showing no internet connection alert
 */
class NetworkAlertDialog(context: Context) : Dialog(context) {

    private var onTryAgainListener: (() -> Unit)? = null
    private var onCancelListener: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_no_internet)
        
        // Set dialog window properties
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window?.setLayout(
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
        setCancelable(false)
        
        // Initialize views
        val btnTryAgain = findViewById<Button>(R.id.btnTryAgain)
        val btnCancel = findViewById<Button>(R.id.btnCancel)
        
        // Set click listeners
        btnTryAgain.setOnClickListener {
            onTryAgainListener?.invoke()
            dismiss()
        }
        
        btnCancel.setOnClickListener {
            onCancelListener?.invoke()
            dismiss()
        }
    }

    /**
     * Set the listener for Try Again button
     */
    fun setOnTryAgainListener(listener: () -> Unit): NetworkAlertDialog {
        onTryAgainListener = listener
        return this
    }

    /**
     * Set the listener for Cancel button
     */
    fun setOnCancelListener(listener: () -> Unit): NetworkAlertDialog {
        onCancelListener = listener
        return this
    }

    companion object {
        /**
         * Show the no internet connection dialog
         * @param context Application context
         * @param onTryAgain Callback when Try Again is clicked
         * @param onCancel Callback when Cancel is clicked
         */
        fun show(
            context: Context,
            onTryAgain: (() -> Unit)? = null,
            onCancel: (() -> Unit)? = null
        ): NetworkAlertDialog {
            val dialog = NetworkAlertDialog(context)
            
            onTryAgain?.let {
                dialog.setOnTryAgainListener(it)
            }
            
            onCancel?.let {
                dialog.setOnCancelListener(it)
            }
            
            dialog.show()
            return dialog
        }
    }
}
