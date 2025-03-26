package edu.ismt.prabin.mealmate.ui.common

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import edu.ismt.prabin.mealmate.R

/**
 * A reusable dialog fragment for confirming deletion actions.
 */
class DeleteConfirmationDialog : DialogFragment() {

    private var title: String? = null
    private var message: String? = null
    private var onConfirmListener: (() -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return MaterialAlertDialogBuilder(requireContext())
            .setTitle(title ?: getString(R.string.delete_confirmation_title))
            .setMessage(message ?: getString(R.string.delete_confirmation_message))
            .setNegativeButton(R.string.cancel) { _, _ -> dismiss() }
            .setPositiveButton(R.string.delete) { _, _ ->
                onConfirmListener?.invoke()
                dismiss()
            }
            .create()
    }

    companion object {
        const val TAG = "DeleteConfirmationDialog"

        /**
         * Create a new instance of the dialog with custom title, message, and confirm action.
         */
        fun newInstance(
            title: String? = null,
            message: String? = null,
            onConfirm: () -> Unit
        ): DeleteConfirmationDialog {
            return DeleteConfirmationDialog().apply {
                this.title = title
                this.message = message
                this.onConfirmListener = onConfirm
            }
        }
    }
} 