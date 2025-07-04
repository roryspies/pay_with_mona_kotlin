package ng.mona.paywithmona.presentation.shared

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ng.mona.paywithmona.presentation.theme.SdkTheme

@Composable
internal fun SdkButton(
    text: String,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
    onClick: () -> Unit = {},
    additionalContent: @Composable (RowScope.() -> Unit)? = null,
) {
    val contentColor = Color(0xFFF4FCF5)
    Button(
        modifier = modifier.heightIn(52.dp),
        onClick = onClick,
        enabled = !loading,
        shape = RoundedCornerShape(4.dp),
        colors = ButtonDefaults.buttonColors(
            contentColor = contentColor,
            disabledContainerColor = MaterialTheme.colorScheme.primary.copy(0.8f)
        ),
        content = {
            AnimatedContent(
                targetState = loading,
                content = { state ->
                    when (state) {
                        true -> CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = contentColor
                        )

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