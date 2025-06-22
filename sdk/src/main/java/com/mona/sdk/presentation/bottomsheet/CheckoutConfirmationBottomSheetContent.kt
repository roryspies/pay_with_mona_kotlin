package com.mona.sdk.presentation.bottomsheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.mona.sdk.data.model.Bank
import com.mona.sdk.domain.PaymentMethod
import com.mona.sdk.presentation.PaymentMethodItem
import com.mona.sdk.presentation.PaymentMethodItemType
import com.mona.sdk.presentation.shared.SdkButton
import com.mona.sdk.presentation.theme.SdkColors
import com.mona.sdk.presentation.theme.SdkTheme
import com.mona.sdk.util.format

@Composable
internal fun CheckoutConfirmationBottomSheetContent(
    method: PaymentMethod.SavedInfo,
    amount: Int,
    modifier: Modifier = Modifier,
    onPay: () -> Unit,
    onChange: () -> Unit,
) {
    Column(
        modifier = modifier,
        content = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(8.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = {
                    Text(
                        text = "Amount to pay",
                        color = Color(0xFF9A9A93),
                        fontWeight = FontWeight.W400
                    )
                    Text(
                        text = amount.format(),
                        color = SdkColors.darkText,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.W700
                    )
                }
            )

            Text(
                modifier = Modifier.padding(vertical = 16.dp),
                text = "Payment Method",
                fontSize = 16.sp,
                fontWeight = FontWeight.W600,
                color = SdkColors.darkText
            )

            PaymentMethodItem(
                entry = method,
                type = PaymentMethodItemType.Confirmation,
                onClick = onChange
            )

            SdkButton(
                modifier = Modifier.padding(top = 27.dp).fillMaxWidth(),
                text = "Pay",
                onClick = onPay,
            )
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun CheckoutConfirmationBottomSheetContentPreview() = SdkTheme {
    CheckoutConfirmationBottomSheetContent(
        method = PaymentMethod.SavedInfo(
            bank = Bank(
                id = "bank_123",
                name = "Test Bank",
                isPrimary = true,
                logo = "https://example.com/logo.png"
            )
        ),
        amount = 20000,
        onPay = {},
        onChange = {},
    )
}