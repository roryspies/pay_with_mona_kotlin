package com.mona.sdk.presentation.shared.bottomsheet

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mona.sdk.R

@Composable
internal fun BottomSheetHeader(
    modifier: Modifier = Modifier,
    showCancelButton: Boolean = true,
    isForCustomTab: Boolean = false,
    onCancelButtonTap: (() -> Unit)? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
            .background(MaterialTheme.colorScheme.primary),
        content = {
            Image(
                painter = painterResource(id = R.drawable.img_lagos_city),
                contentDescription = null,
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxSize()
            )

            if (isForCustomTab || showCancelButton) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp)
                        .size(24.dp)
                        .clickable { onCancelButtonTap?.invoke() },
                    content = {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White,
                            modifier = Modifier.fillMaxSize(),
                            content = {
                                // SVG icon
//                                SvgIcon(
//                                    resId = R.drawable.ic_x, // Replace with your SVG resource
//                                    modifier = Modifier
//                                        .size(21.dp)
//                                        .align(Alignment.Center)
//                                )
                            }
                        )
                    }
                )
            }
        }
    )
}