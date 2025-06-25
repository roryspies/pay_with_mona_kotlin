package ng.mona.paywithmona.presentation.bottomsheet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ng.mona.paywithmona.data.model.Collection
import ng.mona.paywithmona.data.model.CollectionSchedule
import ng.mona.paywithmona.data.model.CollectionScheduleEntry
import ng.mona.paywithmona.data.model.CollectionType
import ng.mona.paywithmona.presentation.shared.SdkButton
import ng.mona.paywithmona.presentation.theme.SdkTheme

@Composable
internal fun CollectionConfirmationBottomSheetContent(
    collection: Collection,
    merchantName: String,
    success: Boolean,
    modifier: Modifier = Modifier,
    onContinue: () -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        content = {
            CollectionBottomSheetHeader(
                type = when (success) {
                    true -> CollectionBottomSheetHeaderType.Success
                    else -> CollectionBottomSheetHeaderType.Default
                }
            )
            Text(
                modifier = Modifier.padding(top = 24.dp, bottom = 2.dp),
                text = if (success) "Your automatic payments are confirmed" else "$merchantName wants to automate repayments",
                textAlign = TextAlign.Center,
                fontSize = 16.sp,
                fontWeight = FontWeight.W600,
                color = Color(0xFF1A1A1A)
            )
            Text(
                text = if (success) "See the details below" else "Please verify the details below",
                textAlign = TextAlign.Center,
                fontSize = 14.sp,
                color = Color(0xFF666666)
            )

            CollectionDetailsGrid(
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                merchantName = merchantName,
                collection = collection
            )

            SdkButton(
                modifier = Modifier.fillMaxWidth(),
                text = if (success) "Continue" else "Continue to account selection",
                onClick = onContinue
            )
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun CollectionConfirmationBottomSheetContentPreview() = SdkTheme {
    CollectionConfirmationBottomSheetContent(
        merchantName = "Mona Merchant",
        collection = Collection(
            id = "12345",
            maxAmountInKobo = "100000",
            startDate = "2023-01-01T00:00:00Z",
            expiryDate = "2024-01-01T00:00:00Z",
            schedule = CollectionSchedule(
                type = CollectionType.Scheduled.name,
                entries = listOf(
                    CollectionScheduleEntry(amountInKobo = "10000", date = "2023-01-01T00:00:00Z"),
                    CollectionScheduleEntry(amountInKobo = "20000", date = "2023-02-01T00:00:00Z")
                ),
                frequency = "Monthly"
            ),
            reference = "REF12345"
        ),
        success = false,
        onContinue = {

        }
    )
}

@Preview(showBackground = true)
@Composable
private fun CollectionSuccessBottomSheetContentPreview() = SdkTheme {
    CollectionConfirmationBottomSheetContent(
        merchantName = "Mona Merchant",
        collection = Collection(
            id = "12345",
            maxAmountInKobo = "100000",
            startDate = "2023-01-01T00:00:00Z",
            expiryDate = "2024-01-01T00:00:00Z",
            schedule = CollectionSchedule(
                type = CollectionType.Scheduled.name,
                entries = listOf(
                    CollectionScheduleEntry(amountInKobo = "10000", date = "2023-01-01T00:00:00Z"),
                    CollectionScheduleEntry(amountInKobo = "20000", date = "2023-02-01T00:00:00Z")
                ),
                frequency = "Monthly"
            ),
            reference = "REF12345"
        ),
        success = true,
        onContinue = {

        }
    )
}