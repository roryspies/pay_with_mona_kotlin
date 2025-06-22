package com.mona.sdk.service.bottomsheet

import android.app.Activity
import android.view.ViewGroup
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.mona.sdk.R
import com.mona.sdk.domain.MonaSdkState
import com.mona.sdk.domain.PaymentMethod
import com.mona.sdk.event.TransactionState
import com.mona.sdk.presentation.bottomsheet.CheckoutConfirmationBottomSheetContent
import com.mona.sdk.presentation.bottomsheet.CheckoutInitiatedBottomSheetContent
import com.mona.sdk.presentation.bottomsheet.KeyExchangeBottomSheetContent
import com.mona.sdk.presentation.bottomsheet.LoadingBottomSheetContent
import com.mona.sdk.presentation.shared.PoweredByMona
import com.mona.sdk.presentation.theme.SdkTheme
import com.mona.sdk.util.lighten
import com.mona.sdk.util.setNavigationBarColor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class BottomSheetHandler(
    private val scope: CoroutineScope,
    private val state: () -> MonaSdkState,
    private val transactionState: () -> StateFlow<TransactionState>,
) {
    private val content = MutableStateFlow<BottomSheetContent?>(null)
    private var dialog: BottomSheetDialog? = null

    private val _response = MutableSharedFlow<BottomSheetResponse>()
    val response: SharedFlow<BottomSheetResponse>
        get() = _response

    fun show(content: BottomSheetContent, activity: Activity? = null) {
        scope.launch(Dispatchers.Main) {
            this@BottomSheetHandler.content.update { content }
            if (dialog == null && activity != null) {
                dialog = buildDialog(activity)
                dialog?.show()
            }
        }
    }

    fun dismiss() {
        updateResponse(BottomSheetResponse.Dismissed)
        content.update { null }
        dialog?.dismiss()
        dialog = null
    }

    @Composable
    private fun Content(modifier: Modifier = Modifier) {
        val content by content.collectAsStateWithLifecycle()
        val background by animateColorAsState(
            targetValue = when (content) {
                BottomSheetContent.CheckoutConfirmation -> MaterialTheme.colorScheme.background
                else -> MaterialTheme.colorScheme.surface
            },
            label = "BottomSheetBackgroundColor"
        )
        setNavigationBarColor(background)

        Column(
            modifier = modifier
                .fillMaxWidth()
                .background(background, RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp))
                .padding(bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = {
                Header(
                    showCancelButton = when (content) {
                        BottomSheetContent.OtpInput,
                        BottomSheetContent.CheckoutInitiated,
                        BottomSheetContent.CollectionSuccess,
                        BottomSheetContent.CheckoutFailure,
                        BottomSheetContent.Loading -> false

                        else -> true
                    }
                )
                AnimatedContent(
                    modifier = Modifier
                        .padding(vertical = 20.dp, horizontal = 16.dp)
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center,
                    targetState = content,
                    content = { current ->
                        when (current) {
                            BottomSheetContent.Loading -> LoadingBottomSheetContent()

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
                                    tint = Color(0xFF090901),
                                )
                            }
                        )
                    }
                )
            }
        )
    }

    private fun buildDialog(activity: Activity) = BottomSheetDialog(activity).apply {
        setCancelable(false)
        setCanceledOnTouchOutside(false)
        behavior.isDraggable = true
        behavior.isHideable = false
        behavior.skipCollapsed = true
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        setOnShowListener {
            findViewById<ViewGroup>(com.google.android.material.R.id.design_bottom_sheet)
                ?.setBackgroundResource(android.R.color.transparent)
        }
        setContentView(
            ComposeView(activity).apply {
                setContent {
                    SdkTheme {
                        this@BottomSheetHandler.Content()
                    }
                }
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
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