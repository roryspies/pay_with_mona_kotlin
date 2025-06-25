package ng.mona.paywithmona.service.bottomsheet

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import ng.mona.paywithmona.R
import ng.mona.paywithmona.domain.PayWithMonaSdkState
import ng.mona.paywithmona.domain.PaymentMethod
import ng.mona.paywithmona.event.TransactionState
import ng.mona.paywithmona.presentation.bottomsheet.CheckoutCompleteBottomSheetContent
import ng.mona.paywithmona.presentation.bottomsheet.CheckoutConfirmationBottomSheetContent
import ng.mona.paywithmona.presentation.bottomsheet.CheckoutInitiatedBottomSheetContent
import ng.mona.paywithmona.presentation.bottomsheet.CollectionAccountSelectionBottomSheetContent
import ng.mona.paywithmona.presentation.bottomsheet.CollectionConfirmationBottomSheetContent
import ng.mona.paywithmona.presentation.bottomsheet.KeyExchangeBottomSheetContent
import ng.mona.paywithmona.presentation.bottomsheet.LoadingBottomSheetContent
import ng.mona.paywithmona.presentation.bottomsheet.OtpInputBottomSheetContent
import ng.mona.paywithmona.presentation.shared.PoweredByMona
import ng.mona.paywithmona.presentation.theme.SdkColors
import ng.mona.paywithmona.util.lighten
import ng.mona.paywithmona.util.setNavigationBarColor

internal class BottomSheetHandler(
    private val scope: CoroutineScope,
    private val state: () -> PayWithMonaSdkState,
    private val transactionState: () -> StateFlow<TransactionState>,
) {
    private val content = MutableStateFlow<BottomSheetContent?>(null)
    private var fragment: DialogFragment? = null

    private val _response = MutableSharedFlow<BottomSheetResponse>()
    val response: SharedFlow<BottomSheetResponse>
        get() = _response

    fun show(content: BottomSheetContent, activity: FragmentActivity? = null) {
        scope.launch(Dispatchers.Main) {
            this@BottomSheetHandler.content.update { content }
            if (fragment == null && activity != null) {
                fragment = DialogFragment {
                    Content()
                }
                fragment?.show(activity.supportFragmentManager, "PayWithMonaBottomSheet")
            }
        }
    }

    fun dismiss() {
        updateResponse(BottomSheetResponse.Dismissed)
        content.update { null }
        fragment?.dismiss()
        fragment = null
    }

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    private fun Content(modifier: Modifier = Modifier) {
        val content by content.collectAsStateWithLifecycle()
        val background by animateColorAsState(
            targetValue = when (content) {
                is BottomSheetContent.OtpInput, BottomSheetContent.CheckoutConfirmation -> MaterialTheme.colorScheme.background
                else -> MaterialTheme.colorScheme.surface
            },
            label = "BottomSheetBackgroundColor"
        )
        setNavigationBarColor(background)

        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(background, RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                .padding(bottom = 16.dp)
                .imePadding()
                .windowInsetsPadding(WindowInsets.ime)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            content = {
                Header(
                    showCancelButton = when (content) {
                        BottomSheetContent.CheckoutInitiated,
                        BottomSheetContent.CollectionSuccess,
                        BottomSheetContent.CheckoutFailure,
                        BottomSheetContent.Loading -> false

                        else -> true
                    }
                )
                AnimatedContent(
                    modifier = Modifier
                        .padding(horizontal = 16.dp)
                        .fillMaxWidth()
                        .weight(1f, false)
                        .verticalScroll(rememberScrollState())
                        .background(MaterialTheme.colorScheme.background)
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                    targetState = content,
                    transitionSpec = {
                        (fadeIn(animationSpec = tween(220, delayMillis = 90)) +
                                scaleIn(
                                    initialScale = 0.92f,
                                    animationSpec = tween(220, delayMillis = 90)
                                )).togetherWith(
                            fadeOut(animationSpec = tween(90))
                        )
                    },
                    content = { current ->
                        when (current) {
                            BottomSheetContent.Loading -> LoadingBottomSheetContent()

                            is BottomSheetContent.OtpInput -> OtpInputBottomSheetContent(
                                title = current.title,
                                length = current.length,
                                isPassword = current.isPassword,
                                onClose = ::dismiss,
                                onDone = { otp ->
                                    updateResponse(BottomSheetResponse.Otp(otp))
                                }
                            )

                            BottomSheetContent.KeyExchange -> KeyExchangeBottomSheetContent {
                                updateResponse(BottomSheetResponse.CanEnrol)
                            }

                            BottomSheetContent.CheckoutConfirmation -> CheckoutConfirmationBottomSheetContent(
                                amount = state().checkout?.transactionAmountInKobo ?: 0,
                                method = state().method as PaymentMethod.SavedInfo,
                                onChange = ::dismiss,
                                onPay = {
                                    updateResponse(BottomSheetResponse.Pay)
                                },
                            )

                            BottomSheetContent.CheckoutInitiated -> {
                                val state by transactionState().collectAsStateWithLifecycle()
                                CheckoutInitiatedBottomSheetContent(
                                    state,
                                    onDone = {
                                        when (state) {
                                            is TransactionState.Completed -> show(BottomSheetContent.CheckoutSuccess)
                                            is TransactionState.Failed -> show(BottomSheetContent.CheckoutFailure)
                                            else -> {
                                                // no-op
                                            }
                                        }
                                    }
                                )
                            }

                            BottomSheetContent.CheckoutSuccess -> {
                                val onAction = {
                                    updateResponse(
                                        BottomSheetResponse.CheckoutComplete(
                                            true
                                        )
                                    )
                                }

                                LaunchedEffect(Unit) {
                                    delay(2000L)
                                    onAction()
                                }

                                CheckoutCompleteBottomSheetContent(
                                    success = true,
                                    amount = state().checkout?.transactionAmountInKobo ?: 0,
                                    onAction = onAction
                                )
                            }

                            BottomSheetContent.CheckoutFailure -> CheckoutCompleteBottomSheetContent(
                                success = false,
                                amount = state().checkout?.transactionAmountInKobo ?: 0,
                                onAction = {
                                    updateResponse(
                                        BottomSheetResponse.CheckoutComplete(
                                            false
                                        )
                                    )
                                }
                            )

                            is BottomSheetContent.CollectionConfirmation -> CollectionConfirmationBottomSheetContent(
                                merchantName = current.merchantName,
                                collection = current.collection,
                                success = false,
                                onContinue = {
                                    updateResponse(BottomSheetResponse.ToCollectionAccountSelection)
                                }
                            )

                            is BottomSheetContent.CollectionAccountSelection -> CollectionAccountSelectionBottomSheetContent(
                                merchantName = current.merchantName,
                                collection = current.collection,
                                paymentOptions = current.paymentOptions,
                                onContinue = { approved ->
                                    updateResponse(
                                        when (approved) {
                                            true -> BottomSheetResponse.ApproveCollectionDebiting
                                            else -> BottomSheetResponse.Dismissed
                                        }
                                    )
                                },
                                onAddAccount = {
                                    updateResponse(BottomSheetResponse.AddBankAccount)
                                }
                            )

                            else -> {
                                // no-op
                            }
                        }
                    }
                )
                PoweredByMona()
            }
        )
    }

    @Composable
    private fun Header(
        modifier: Modifier = Modifier,
        showCancelButton: Boolean = true,
    ) {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                .fillMaxWidth()
                .height(40.dp)
                .background(MaterialTheme.colorScheme.primary),
            content = {
                Image(
                    painter = painterResource(id = R.drawable.img_lagos_city),
                    contentDescription = null,
                    contentScale = ContentScale.FillWidth,
                    modifier = Modifier.fillMaxSize()
                )

                AnimatedVisibility(
                    modifier = Modifier.align(Alignment.CenterEnd).padding(end = 16.dp),
                    visible = showCancelButton,
                    content = {
                        Box(
                            modifier = Modifier.size(24.dp)
                                .background(
                                    MaterialTheme.colorScheme.primary.lighten(35f),
                                    RoundedCornerShape(50)
                                )
                                .clickable(onClick = ::dismiss),
                            contentAlignment = Alignment.Center,
                            content = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_close),
                                    contentDescription = "Dismiss",
                                    tint = SdkColors.neutral900,
                                )
                            }
                        )
                    }
                )
            }
        )
    }

    private fun updateResponse(response: BottomSheetResponse) {
        scope.launch {
            _response.emit(response)
        }
    }
}