package ng.mona.paywithmona.presentation.bottomsheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ng.mona.paywithmona.R
import ng.mona.paywithmona.presentation.shared.MerchantLogo
import ng.mona.paywithmona.presentation.theme.SdkColors

internal enum class CollectionBottomSheetHeaderType {
    Success,
    Pending,
    Default
}

@Composable
internal fun CollectionBottomSheetHeader(
    type: CollectionBottomSheetHeaderType,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = {
            val color = when (type) {
                CollectionBottomSheetHeaderType.Pending -> SdkColors.warning
                else -> MaterialTheme.colorScheme.primary
            }
            Box(
                modifier = Modifier.size(48.dp).background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center,
                content = {
                    Icon(
                        modifier = Modifier.size(28.dp),
                        painter = painterResource(
                            id = when (type) {
                                CollectionBottomSheetHeaderType.Success -> R.drawable.ic_confetti
                                CollectionBottomSheetHeaderType.Pending -> R.drawable.ic_hourglass
                                CollectionBottomSheetHeaderType.Default -> R.drawable.ic_bank
                            }
                        ),
                        contentDescription = null,
                        tint = color
                    )
                }
            )
            if (type == CollectionBottomSheetHeaderType.Default) {
                Icon(
                    modifier = Modifier.height(22.dp),
                    painter = painterResource(id = R.drawable.ic_forback),
                    contentDescription = "Arrow",
                    tint = SdkColors.neutral100,
                )
                MerchantLogo()
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun CollectionBottomSheetHeaderPreview() {
    CollectionBottomSheetHeader(type = CollectionBottomSheetHeaderType.Default)
}

@Preview(showBackground = true)
@Composable
private fun CollectionPendingBottomSheetHeaderPreview() {
    CollectionBottomSheetHeader(type = CollectionBottomSheetHeaderType.Pending)
}

@Preview(showBackground = true)
@Composable
private fun CollectionSuccessBottomSheetHeaderPreview() {
    CollectionBottomSheetHeader(type = CollectionBottomSheetHeaderType.Success)
}