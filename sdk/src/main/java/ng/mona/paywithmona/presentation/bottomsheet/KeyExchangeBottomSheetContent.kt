package ng.mona.paywithmona.presentation.bottomsheet

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ng.mona.paywithmona.R
import ng.mona.paywithmona.presentation.shared.MerchantLogo
import ng.mona.paywithmona.presentation.shared.SdkButton
import ng.mona.paywithmona.presentation.theme.SdkColors
import ng.mona.paywithmona.presentation.theme.SdkTheme
import ng.mona.paywithmona.util.lighten

@Composable
internal fun KeyExchangeBottomSheetContent(
    modifier: Modifier = Modifier,
    onSetUp: () -> Unit
) {
    Column(
        modifier = modifier.padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = {
            Row(
                modifier = Modifier.padding(top = 20.dp, bottom = 24.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                content = {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFE7048)),
                        contentAlignment = Alignment.Center,
                        content = {
                            Image(
                                painter = painterResource(id = R.drawable.ic_logo),
                                contentDescription = null,
                            )
                        }
                    )
                    Icon(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        painter = painterResource(R.drawable.path_checkmark),
                        contentDescription = null,
                        tint = Color(0xFFC6C7C3)
                    )
                    MerchantLogo()
                }
            )

            Text(
                text = "One Last Thing! ",
                fontSize = MaterialTheme.typography.titleLarge.fontSize,
                fontWeight = FontWeight.W600
            )

            Text(
                modifier = Modifier.padding(top = 2.dp, bottom = 16.dp),
                text = "Set up biometrics for faster, one-tap payments — every time you check out.",
                fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                color = SdkColors.subText,
                textAlign = TextAlign.Center
            )

            Row(
                modifier = Modifier.background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    RoundedCornerShape(4.dp)
                ).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                content = {
                    Icon(
                        painter = painterResource(R.drawable.ic_safe),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.lighten(35f)
                    )
                    Text(
                        text = "This is to make sure that you are the only one who can authorize payments.",
                        color = MaterialTheme.colorScheme.primary.lighten(35f),
                        modifier = Modifier.weight(1f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.W500,
                        lineHeight = 16.sp,
                    )
                }
            )
            SdkButton(
                modifier = Modifier.padding(top = 24.dp).fillMaxWidth(),
                text = "Set Up",
                onClick = onSetUp
            )
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun KeyExchangeBottomSheetContentPreview() = SdkTheme {
    KeyExchangeBottomSheetContent(
        onSetUp = {}
    )
}