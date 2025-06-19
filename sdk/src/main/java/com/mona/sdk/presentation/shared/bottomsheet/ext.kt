package com.mona.sdk.presentation.shared.bottomsheet

import android.app.Activity
import android.app.Dialog
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.mona.sdk.presentation.theme.SdkTheme

internal fun showBottomSheet(
    activity: Activity?,
    isDismissible: Boolean = true,
    enableDrag: Boolean = true,
    showCancelButton: Boolean = true,
    isForCustomTab: Boolean = false,
    onCancelButtonTap: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null,
    content: @Composable () -> Unit
): Dialog? {
    return BottomSheetDialog(activity ?: return null).apply {
        setCancelable(isDismissible)
        setCanceledOnTouchOutside(isDismissible)
        behavior.isDraggable = enableDrag
        setContentView(
            ComposeView(activity).apply {
                setContent {
                    SdkTheme {
                        BottomSheetWrapper(
                            isForCustomTab = isForCustomTab,
                            showCancelButton = showCancelButton,
                            onCancelButtonTap = {
                                onCancelButtonTap?.invoke()
                                dismiss()
                            },
                            content = content
                        )
                    }
                }
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }
        )
        setOnDismissListener {
            onDismiss?.invoke()
        }
        show()
    }
}
