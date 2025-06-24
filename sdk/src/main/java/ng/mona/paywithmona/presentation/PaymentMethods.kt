package ng.mona.paywithmona.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import ng.mona.paywithmona.R
import ng.mona.paywithmona.data.model.PaymentOptions
import ng.mona.paywithmona.domain.PaymentMethod
import ng.mona.paywithmona.presentation.shared.SdkButton
import ng.mona.paywithmona.presentation.theme.SdkColors
import ng.mona.paywithmona.presentation.theme.SdkTheme

@Composable
internal fun PaymentMethods(
    options: PaymentOptions?,
    modifier: Modifier = Modifier,
    loading: Boolean = false,
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
            .background(SdkColors.white)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp),
        content = {
            Text(
                text = "Payment Methods",
                fontSize = 16.sp,
                fontWeight = FontWeight.W500,
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                content = {
                    methods.forEach { method ->
                        PaymentMethodItem(
                            entry = method,
                            selected = selectedMethod == method,
                            type = PaymentMethodItemType.Methods,
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
                loading = loading,
                additionalContent = when (selectedMethod) {
                    is PaymentMethod.SavedInfo -> {
                        {
                            val method = selectedMethod as PaymentMethod.SavedInfo
                            Box(
                                modifier = Modifier.padding(horizontal = 20.dp).size(1.dp, 20.dp)
                                    .background(SdkColors.white)
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

@Preview
@Composable
private fun PaymentMethodsPreview() = SdkTheme {
    PaymentMethods(null) {}
}