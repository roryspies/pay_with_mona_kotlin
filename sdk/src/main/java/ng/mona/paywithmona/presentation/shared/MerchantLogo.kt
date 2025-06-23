package ng.mona.paywithmona.presentation.shared

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import ng.mona.paywithmona.PayWithMonaSdk
import ng.mona.paywithmona.presentation.theme.SdkTheme
import ng.mona.paywithmona.util.appIconPainter

@Composable
internal fun MerchantLogo(
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
) {
    val branding by PayWithMonaSdk.merchantBranding.collectAsState(null)
    var loadingError by remember { mutableStateOf(false) }
    val modifier = modifier
        .size(size)
        .clip(CircleShape)

    AnimatedContent(
        targetState = branding != null && !loadingError,
        content = { available ->
            when (available) {
                true -> AsyncImage(
                    modifier = modifier,
                    model = branding?.image,
                    contentDescription = branding?.name,
                    onError = {
                        loadingError = true
                    }
                )

                else -> Box(
                    modifier = modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center,
                    content = {
                        val painter = appIconPainter()
                        if (painter != null) {
                            Image(
                                painter = painter,
                                contentDescription = null,
                            )
                        }
                    }
                )
            }
        }
    )
}

@Preview
@Composable
private fun MerchantLogoPreview() = SdkTheme {
    MerchantLogo()
}