package com.mona.sdk.presentation.shared.bottomsheet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

@Composable
fun BottomSheetWrapper(
    modifier: Modifier = Modifier,
    showCancelButton: Boolean = true,
    isForCustomTab: Boolean = false,
    onCancelButtonTap: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val insets = ViewCompat.getRootWindowInsets(view)
    val bottomInset = with(LocalDensity.current) {
        (insets?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0).toDp()
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(bottom = bottomInset),
        color = Color.White,
        shape = RoundedCornerShape(
            topStart = 16.dp,
            topEnd = 16.dp
        ),
        content = {
            Column {
                BottomSheetHeader(
                    isForCustomTab = isForCustomTab,
                    showCancelButton = showCancelButton,
                    onCancelButtonTap = onCancelButtonTap
                )
                content()
            }
        }
    )
}