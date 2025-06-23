package ng.mona.paywithmona.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import ng.mona.paywithmona.R
import ng.mona.paywithmona.data.model.Bank
import ng.mona.paywithmona.data.model.Card
import ng.mona.paywithmona.domain.PaymentMethod
import ng.mona.paywithmona.presentation.theme.SdkColors
import ng.mona.paywithmona.presentation.theme.SdkTheme

internal enum class PaymentMethodItemType {
    Methods,
    Confirmation,
}

@Composable
internal fun PaymentMethodItem(
    entry: PaymentMethod,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    type: PaymentMethodItemType,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier.then(
            when (type) {
                PaymentMethodItemType.Methods -> Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                )

                else -> Modifier
            }
        ),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = {
            when (entry) {
                PaymentMethod.PayByTransfer, PaymentMethod.PayWithCard -> {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center,
                        content = {
                            Icon(
                                modifier = Modifier.size(19.dp),
                                painter = painterResource(
                                    id = when (entry) {
                                        PaymentMethod.PayByTransfer -> R.drawable.ic_bank
                                        else -> R.drawable.ic_card
                                    }
                                ),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    )
                }

                is PaymentMethod.SavedInfo -> {
                    Box(
                        content = {
                            AsyncImage(
                                model = (entry.bank?.logo ?: entry.card?.logo),
                                contentDescription = entry.bank?.name ?: entry.card?.bankName,
                                modifier = Modifier.size(36.dp),
                            )
                            Icon(
                                modifier = Modifier.align(Alignment.BottomEnd)
                                    .background(SdkColors.white, CircleShape)
                                    .padding(2.dp)
                                    .size(10.dp),
                                painter = painterResource(if (entry.bank != null) R.drawable.ic_bank_bold else R.drawable.ic_cards),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                content = {
                    Text(
                        text = when (entry) {
                            is PaymentMethod.SavedInfo -> entry.bank?.name
                                ?: entry.card?.bankName ?: stringResource(R.string.n_a)

                            PaymentMethod.PayByTransfer -> stringResource(R.string.pay_by_transfer)
                            PaymentMethod.PayWithCard -> stringResource(R.string.pay_with_card)
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.W500,
                    )
                    Row(
                        content = {
                            val leading = when {
                                type == PaymentMethodItemType.Methods && entry is PaymentMethod.SavedInfo -> {
                                    stringResource(
                                        if (entry.bank != null) {
                                            R.string.account
                                        } else R.string.card
                                    ) + " • "
                                }

                                else -> ""
                            }
                            Text(
                                text = when (entry) {
                                    is PaymentMethod.SavedInfo -> leading + (
                                            entry.bank?.accountNumber
                                                ?: entry.card?.accountNumber
                                                ?: stringResource(R.string.n_a)
                                            )

                                    PaymentMethod.PayByTransfer -> stringResource(R.string.pay_by_transfer_desc)
                                    PaymentMethod.PayWithCard -> stringResource(R.string.pay_with_card_desc)
                                },
                                fontSize = 12.sp,
                                color = when (entry) {
                                    is PaymentMethod.SavedInfo -> SdkColors.subText
                                    else -> Color(0xFF999999)
                                }
                            )
                        }
                    )
                }
            )
            when (type) {
                PaymentMethodItemType.Methods -> RadioButton(
                    selected = selected,
                    onClick = null,
                    colors = RadioButtonDefaults.colors(unselectedColor = Color(0xFFF2F2F3))
                )

                PaymentMethodItemType.Confirmation -> Row(
                    modifier = Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    content = {
                        Text(
                            text = "Change",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.W600,
                        )
                        Icon(
                            modifier = Modifier.size(12.dp),
                            painter = painterResource(R.drawable.ic_caret_right),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                )
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
private fun PaymentMethodItemPreview() = SdkTheme {
    PaymentMethodItem(
        entry = PaymentMethod.PayByTransfer,
        type = PaymentMethodItemType.Methods,
        onClick = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun SelectedPaymentMethodItemPreview() = SdkTheme {
    PaymentMethodItem(
        entry = PaymentMethod.PayWithCard,
        type = PaymentMethodItemType.Confirmation,
        selected = true,
        onClick = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun BankPaymentMethodItemPreview() = SdkTheme {
    PaymentMethodItem(
        entry = PaymentMethod.SavedInfo(
            bank = Bank(
                name = "Bank of Mona",
                accountNumber = "1234567890"
            )
        ),
        type = PaymentMethodItemType.Methods,
        selected = true,
        onClick = {},
    )
}

@Preview(showBackground = true)
@Composable
private fun CardPaymentMethodItemPreview() = SdkTheme {
    PaymentMethodItem(
        entry = PaymentMethod.SavedInfo(
            card = Card(
                bankName = "Bank of Mona",
                accountNumber = "1234567890"
            )
        ),
        type = PaymentMethodItemType.Confirmation,
        onClick = {},
    )
}