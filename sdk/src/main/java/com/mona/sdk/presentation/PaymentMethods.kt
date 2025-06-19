package com.mona.sdk.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.mona.sdk.R
import com.mona.sdk.data.model.PaymentOptions
import com.mona.sdk.domain.PaymentMethod
import com.mona.sdk.presentation.shared.SdkButton
import com.mona.sdk.presentation.theme.SdkTheme

@Composable
internal fun PaymentMethods(
    options: PaymentOptions?,
    modifier: Modifier = Modifier,
    onProceed: (PaymentMethod) -> Unit,
) {
    val methods = remember(options) {
        buildList {
            options?.cards?.forEach {
                add(PaymentMethod.SavedInfo(card = it))
            }
            options?.banks?.forEach {
                add(PaymentMethod.SavedInfo(bank = it))
            }
            add(PaymentMethod.PayByTransfer)
            add(PaymentMethod.PayWithCard)
        }
    }
    var selectedMethod by remember(methods) {
        val initial = methods.firstOrNull { method ->
            when (method) {
                is PaymentMethod.SavedInfo -> method.bank?.isPrimary == true
                else -> false
            }
        } ?: methods.first()
        mutableStateOf(initial)
    }

    Column(
        modifier = modifier
            .background(Color.White)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp),
        content = {
            Text(
                text = "Payment Methods",
                fontSize = 16.sp,
                fontWeight = FontWeight.W500,
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(24.dp),
                content = {
                    methods.forEach { method ->
                        PaymentMethodItem(
                            entry = method,
                            selected = selectedMethod == method,
                            onClick = {
                                selectedMethod = method
                            },
                        )
                    }
                }
            )
            SdkButton(
                modifier = Modifier.fillMaxWidth(),
                text = when (selectedMethod) {
                    is PaymentMethod.SavedInfo -> "OneTap"
                    else -> "Proceed to pay"
                },
                additionalContent = when (selectedMethod) {
                    is PaymentMethod.SavedInfo -> {
                        {
                            val method = selectedMethod as PaymentMethod.SavedInfo
                            Box(
                                modifier = Modifier.padding(horizontal = 20.dp).size(1.dp, 20.dp)
                                    .background(Color.White)
                            )
                            AsyncImage(
                                model = (method.bank?.logo ?: method.card?.logo),
                                contentDescription = method.bank?.name ?: method.card?.bankName,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                modifier = Modifier.padding(start = 4.dp),
                                text = (method.bank?.accountNumber
                                    ?: method.card?.accountNumber
                                    ?: stringResource(R.string.n_a)),
                                fontSize = 12.sp,
                            )
                        }
                    }

                    else -> null
                },
                onClick = {
                    onProceed(selectedMethod)
                }
            )
        }
    )
}

@Composable
internal fun PaymentMethodItem(
    entry: PaymentMethod,
    modifier: Modifier = Modifier,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onClick,
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
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .border(2.dp, Color.White, CircleShape),
                                content = {
                                    Icon(
                                        modifier = Modifier.size(14.dp),
                                        painter = painterResource(if (entry.bank != null) R.drawable.ic_bank_bold else R.drawable.ic_cards),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
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
                            Text(
                                text = when (entry) {
                                    is PaymentMethod.SavedInfo -> stringResource(
                                        if (entry.bank != null) {
                                            R.string.account
                                        } else R.string.card
                                    ) + " • " + (entry.bank?.accountNumber
                                        ?: entry.card?.accountNumber
                                        ?: stringResource(R.string.n_a))

                                    PaymentMethod.PayByTransfer -> stringResource(R.string.pay_by_transfer_desc)
                                    PaymentMethod.PayWithCard -> stringResource(R.string.pay_with_card_desc)
                                },
                                fontSize = 12.sp,
                                color = when (entry) {
                                    is PaymentMethod.SavedInfo -> Color(0xFF6A6C60)
                                    else -> Color(0xFF999999)
                                }
                            )
                        }
                    )
                }
            )
            RadioButton(
                selected = selected,
                onClick = null,
                colors = RadioButtonDefaults.colors(unselectedColor = Color(0xFFF2F2F3))
            )
        }
    )
}

@Preview
@Composable
private fun PaymentMethodsPreview() = SdkTheme {
    PaymentMethods(null) {}
}