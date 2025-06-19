package com.mona.sdk.presentation.shared

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mona.sdk.presentation.theme.SdkTheme

@Composable
internal fun SdkButton(
    text: String,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    onClick: () -> Unit = {},
    additionalContent: @Composable (RowScope.() -> Unit)? = null,
) {
    Button(
        modifier = modifier.heightIn(52.dp),
        onClick = onClick,
        shape = RoundedCornerShape(4.dp),
        colors = ButtonDefaults.buttonColors(contentColor = Color(0xFFF4FCF5)),
        content = {
            AnimatedContent(
                targetState = loading,
                content = { state ->
                    when (state) {
                        true -> CircularProgressIndicator()
                        else -> AnimatedContent(
                            targetState = additionalContent != null,
                            content = { hasContent ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    content = {
                                        Text(
                                            text,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.W500,
                                        )
                                        if (hasContent && additionalContent != null) {
                                            additionalContent()
                                        }
                                    }
                                )
                            }
                        )
                    }
                }
            )
        }
    )
}

@Preview
@Composable
private fun SdkButtonPreview() = SdkTheme {
    SdkButton(
        text = "Click Me",
    )
}

@Preview
@Composable
private fun SdkButtonLoadingPreview() = SdkTheme {
    SdkButton(
        text = "Click Me",
        loading = true
    )
}