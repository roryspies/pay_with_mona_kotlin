package ng.mona.paywithmona.service.bottomsheet

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.OnBackPressedCallback
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import ng.mona.paywithmona.presentation.theme.SdkTheme

internal class DialogFragment() : BottomSheetDialogFragment() {
    private var content: (@Composable () -> Unit)? = null
    private var onBackPressedCallback: OnBackPressedCallback? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return (super.onCreateDialog(savedInstanceState) as BottomSheetDialog).apply {
            setCancelable(false)
            setCanceledOnTouchOutside(false)
            behavior.isDraggable = false
            behavior.isHideable = false
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            window?.setBackgroundDrawableResource(android.R.color.transparent)
            window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Do nothing, preventing dialog dismissal
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressedCallback!!)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                SdkTheme {
                    content?.invoke()
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.findViewById<ViewGroup>(com.google.android.material.R.id.design_bottom_sheet)
            ?.setBackgroundResource(android.R.color.transparent)
    }

    override fun onDestroy() {
        onBackPressedCallback?.remove()
        super.onDestroy()
    }

    companion object {
        operator fun invoke(content: @Composable () -> Unit) = DialogFragment().apply {
            this.content = content
        }
    }
}