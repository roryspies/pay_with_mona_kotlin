package ng.mona.paywithmona.presentation.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmapOrNull
import androidx.palette.graphics.Palette
import coil3.asDrawable
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import ng.mona.paywithmona.R
import ng.mona.paywithmona.domain.PaymentMethod
import ng.mona.paywithmona.domain.logo
import ng.mona.paywithmona.domain.name
import ng.mona.paywithmona.domain.number
import ng.mona.paywithmona.presentation.theme.SdkColors


@Composable
internal fun CollectionPaymentItem(
    method: PaymentMethod.SavedInfo,
    selected: Boolean?,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    var imageBackground by remember { mutableStateOf(primary.copy(alpha = 0.2f)) }

    val context = LocalContext.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = {
            Box(
                modifier = Modifier.size(36.dp).background(imageBackground, CircleShape),
                contentAlignment = Alignment.Center,
                content = {
                    AsyncImage(
                        modifier = Modifier.size(28.dp),
                        model = ImageRequest.Builder(context)
                            .data(method.logo)
                            .allowHardware(false)
                            .listener(
                                onSuccess = { _, result ->
                                    // draw the result image on a bitmap
                                    val image =
                                        result.image.asDrawable(context.resources).toBitmapOrNull()
                                            ?: return@listener
                                    Palette.from(image).generate { palette ->
                                        palette?.dominantSwatch?.rgb?.let { colorInt ->
                                            imageBackground = Color(colorInt).copy(alpha = 0.2f)
                                        }
                                    }
                                }
                            )
                            .build(),
                        contentDescription = null
                    )
                }
            )
            Column(
                modifier = Modifier.weight(1f),
                content = {
                    Text(
                        text = method.name ?: stringResource(R.string.n_a),
                        color = SdkColors.darkText,
                        fontWeight = FontWeight.W500,
                        fontSize = 14.sp,
                    )
                    Text(
                        text = method.number ?: stringResource(R.string.n_a),
                        color = SdkColors.subText,
                        fontSize = 12.sp,
                    )
                }
            )
            if (selected != null) {
                RadioButton(
                    selected = selected,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(unselectedColor = SdkColors.neutral50)
                )
            }
        }
    )
}