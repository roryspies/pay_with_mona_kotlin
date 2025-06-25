package ng.mona.paywithmona.presentation.shared

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ng.mona.paywithmona.presentation.theme.SdkColors

@Composable
internal fun ExpandHeader(
    title: String,
    expanded: Boolean,
    modifier: Modifier = Modifier,
    onToggle: () -> Unit
) {
    Row(
        modifier = modifier.clickable(onClick = onToggle),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        content = {
            Text(
                title,
                fontWeight = FontWeight.W500,
                fontSize = 12.sp,
                color = SdkColors.neutral400,
                textDecoration = TextDecoration.Underline
            )
            AnimatedContent(
                targetState = expanded,
                content = { expanded ->
                    Icon(
                        modifier = Modifier.size(12.dp),
                        imageVector = when (expanded) {
                            true -> Icons.Default.KeyboardArrowUp
                            false -> Icons.Default.KeyboardArrowDown
                        },
                        contentDescription = title,
                        tint = SdkColors.neutral400,
                    )
                }
            )
        }
    )
}