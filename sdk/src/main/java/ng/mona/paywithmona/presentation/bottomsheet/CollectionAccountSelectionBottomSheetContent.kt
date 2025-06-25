package ng.mona.paywithmona.presentation.bottomsheet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ng.mona.paywithmona.data.model.Collection
import ng.mona.paywithmona.data.model.CollectionSchedule
import ng.mona.paywithmona.data.model.CollectionScheduleEntry
import ng.mona.paywithmona.data.model.CollectionType
import ng.mona.paywithmona.data.model.PaymentOptions
import ng.mona.paywithmona.domain.PaymentMethod
import ng.mona.paywithmona.presentation.shared.CollectionPaymentItem
import ng.mona.paywithmona.presentation.shared.ExpandHeader
import ng.mona.paywithmona.presentation.shared.SdkButton
import ng.mona.paywithmona.presentation.theme.SdkColors
import ng.mona.paywithmona.presentation.theme.SdkTheme

@Composable
internal fun CollectionAccountSelectionBottomSheetContent(
    collection: Collection,
    merchantName: String,
    paymentOptions: PaymentOptions?,
    modifier: Modifier = Modifier,
    onContinue: (PaymentMethod.SavedInfo?) -> Unit,
    onAddAccount: () -> Unit,
) {
    var showInfo by remember { mutableStateOf(false) }
    val methods = remember(paymentOptions) {
        buildList {
            paymentOptions?.banks?.forEach {
                add(PaymentMethod.SavedInfo(bank = it))
            }
            paymentOptions?.cards?.forEach {
                add(PaymentMethod.SavedInfo(card = it))
            }
        }
    }
    var selectedMethod by remember(methods) {
        val initial = methods.firstOrNull { method ->
            method.bank?.isPrimary == true
        } ?: methods.firstOrNull()
        mutableStateOf(initial)
    }

    val isPending = methods.isEmpty()

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        content = {
            CollectionBottomSheetHeader(
                type = when (isPending) {
                    true -> CollectionBottomSheetHeaderType.Pending
                    else -> CollectionBottomSheetHeaderType.Default
                }
            )
            Text(
                modifier = Modifier.padding(top = 24.dp),
                text = when (isPending) {
                    true -> "Account validation still in progress"
                    else -> "Select payment account"
                },
                textAlign = TextAlign.Center,
                fontSize = 16.sp,
                fontWeight = FontWeight.W600,
                color = SdkColors.darkText
            )
            if (isPending) {
                Text(
                    text = "You currently do not have accounts available for repayments. $merchantName will alert you when it is ready",
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    color = SdkColors.subText
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            if (!isPending) {
                CollectionDetailsGrid(
                    modifier = Modifier.fillMaxWidth(),
                    merchantName = merchantName,
                    collection = collection,
                    state = when (showInfo) {
                        true -> CollectionDetailsGridState.AutoExpanded
                        false -> CollectionDetailsGridState.Collapsed
                    },
                )
                ExpandHeader(
                    modifier = Modifier.fillMaxWidth().padding(top = 5.dp),
                    title = when (showInfo) {
                        true -> "Hide all"
                        else -> "Show all"
                    },
                    expanded = showInfo,
                    onToggle = {
                        showInfo = !showInfo
                    }
                )
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = SdkColors.neutral50
                )
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    content = {
                        when (showInfo) {
                            true -> {
                                selectedMethod?.let {
                                    CollectionPaymentItem(
                                        modifier = Modifier.fillMaxWidth(),
                                        method = it,
                                        selected = true
                                    )
                                }
                            }

                            else -> {
                                methods.forEach {
                                    CollectionPaymentItem(
                                        modifier = Modifier.fillMaxWidth().clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = {
                                                selectedMethod = it
                                            }
                                        ),
                                        method = it,
                                        selected = selectedMethod == it
                                    )
                                }
                            }
                        }
                    }
                )
            }
            Box(
                modifier = Modifier.padding(
                    top = 24.dp,
                    bottom = 27.dp,
                    start = 16.dp,
                    end = 16.dp
                ).fillMaxWidth().clickable(onClick = onAddAccount),
                contentAlignment = Alignment.Center,
                content = {
                    Icon(
                        modifier = Modifier.align(Alignment.CenterStart).size(20.dp),
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = SdkColors.darkText
                    )
                    Text(
                        text = "Add an account",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W500,
                        color = SdkColors.neutral900
                    )
                }
            )
            SdkButton(
                modifier = Modifier.fillMaxWidth(),
                text = if (isPending) "Return to $merchantName" else "Approve debiting",
                onClick = {
                    onContinue(selectedMethod)
                }
            )
            Text(
                modifier = Modifier.padding(top = 10.dp),
                text = "Click “Continue” to agree to $merchantName initiating payments for you within the limits set above through Mona under these Terms of Service",
                fontSize = 10.sp,
                color = SdkColors.subText,
                lineHeight = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun CollectionAccountSelectionBottomSheetContentPreview() = SdkTheme {
    CollectionAccountSelectionBottomSheetContent(
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
        paymentOptions = null,
        onContinue = {

        },
        onAddAccount = {

        }
    )
}