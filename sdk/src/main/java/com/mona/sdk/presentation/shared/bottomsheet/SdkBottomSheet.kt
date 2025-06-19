package com.mona.sdk.presentation.shared.bottomsheet

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SdkBottomSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    isDismissible: Boolean = true,
    enableDrag: Boolean = true,
    showCancelButton: Boolean = true,
    isForCustomTab: Boolean = false,
    onCancelButtonTap: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(isVisible) {
        if (isVisible) {
            coroutineScope.launch { sheetState.show() }
        } else {
            coroutineScope.launch { sheetState.hide() }
        }
    }

    ModalBottomSheet(
        sheetState = sheetState,
        modifier = Modifier,
        onDismissRequest = {

        },
        content = {
            BottomSheetWrapper(
                isForCustomTab = isForCustomTab,
                showCancelButton = showCancelButton,
                onCancelButtonTap = {
                    onCancelButtonTap?.invoke()
                    onDismiss()
                },
                content = content
            )
        }
    )
}