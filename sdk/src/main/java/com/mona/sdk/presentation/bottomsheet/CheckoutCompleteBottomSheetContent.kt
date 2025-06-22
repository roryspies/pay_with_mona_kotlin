package com.mona.sdk.presentation.bottomsheet

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mona.sdk.R
import com.mona.sdk.presentation.shared.SdkButton
import com.mona.sdk.presentation.theme.SdkColors
import com.mona.sdk.presentation.theme.SdkTheme
import com.mona.sdk.util.format

@Composable
internal fun CheckoutCompleteBottomSheetContent(
    success: Boolean,
    amount: Int,
    modifier: Modifier = Modifier,
    onAction: () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .border(0.5.dp, Color(0xFFE7E8E6), CircleShape)
                    .padding(8.dp)
                    .background(
                        when (success) {
                            true -> SdkColors.success
                            else -> SdkColors.error
                        },
                        CircleShape
                    ),
                contentAlignment = Alignment.Center,
                content = {
                    Image(
                        painter = painterResource(
                            id = when (success) {
                                true -> R.drawable.ic_checkmark
                                false -> R.drawable.ic_failed
                            }
                        ),
                        contentDescription = if (success) "Success Icon" else "Failed Icon",
                        modifier = Modifier.size(20.dp)
                    )
                }
            )

            Text(
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                text = when (success) {
                    true -> "Payment Successful!"
                    false -> "Payment Failed!"
                },
                textAlign = TextAlign.Center,
                fontSize = 16.sp,
                fontWeight = FontWeight.W600,
                color = SdkColors.darkText
            )

            Text(
                text = when (success) {
                    true -> "Your payment of ₦${amount.format()} was successful. Mona has sent you a transaction receipt!"
                    false -> "Your payment of ₦${amount.format()} failed!. Please try again or use a different payment method."
                },
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                color = SdkColors.subText,
            )

            Spacer(modifier = Modifier.height(16.dp))

            SdkButton(
                modifier = Modifier.padding(top = 24.dp).fillMaxWidth(),
                onClick = onAction,
                text = when (success) {
                    true -> "Return"
                    false -> "Try Again"
                },
            )
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun SuccessCheckoutCompleteBottomSheetContentPreview() = SdkTheme {
    CheckoutCompleteBottomSheetContent(
        success = true,
        amount = 5000,
        onAction = {}
    )
}

@Preview(showBackground = true)
@Composable
private fun FailureCheckoutCompleteBottomSheetContentPreview() = SdkTheme {
    CheckoutCompleteBottomSheetContent(
        success = false,
        amount = 5000,
        onAction = {}
    )
}