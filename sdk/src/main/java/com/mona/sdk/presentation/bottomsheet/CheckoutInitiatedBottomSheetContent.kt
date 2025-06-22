package com.mona.sdk.presentation.bottomsheet

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.times
import com.mona.sdk.R
import com.mona.sdk.event.TransactionState
import com.mona.sdk.presentation.theme.SdkColors
import com.mona.sdk.presentation.theme.SdkTheme
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
internal fun CheckoutInitiatedBottomSheetContent(
    state: TransactionState,
    modifier: Modifier = Modifier,
    onDone: () -> Unit,
) {
    val currentStep = remember(state) {
        when (state) {
            is TransactionState.Initiated -> ProgressStep.Initiated
            is TransactionState.ProgressUpdate -> ProgressStep.Processing
            else -> ProgressStep.Completed
        }
    }

    val isFailed = state is TransactionState.Failed

    val stepStates = remember(currentStep, isFailed) {
        mapOf(
            ProgressStep.Initiated to StepState(
                isCompleted = currentStep != ProgressStep.Initiated,
                isCurrent = currentStep == ProgressStep.Initiated
            ),
            ProgressStep.Processing to StepState(
                isCompleted = currentStep == ProgressStep.Completed,
                isCurrent = currentStep == ProgressStep.Processing,
                isFailed = isFailed && currentStep == ProgressStep.Completed
            ),
            ProgressStep.Completed to StepState(
                isCompleted = currentStep == ProgressStep.Completed && !isFailed,
                isCurrent = currentStep == ProgressStep.Completed,
                isFailed = isFailed
            )
        )
    }

    LaunchedEffect(state) {
        if (state is TransactionState.Completed || state is TransactionState.Failed) {
            delay(1500) // Show completion state briefly
            onDone()
        }
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = {
            Text(
                text = "Hang Tight, We're On It!",
                fontSize = 16.sp,
                fontWeight = FontWeight.W500,
                color = SdkColors.darkText,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Text(
                text = "Your transfer is on the way—we'll confirm as soon as it lands.",
                fontSize = 12.sp,
                color = SdkColors.subText,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )


            Layout(
                modifier = Modifier.fillMaxWidth(),
                content = {
                    // Step 1: Initiated
                    ProgressBar(
                        state = stepStates[ProgressStep.Initiated]!!
                    )

                    StepIcon(
                        state = stepStates[ProgressStep.Initiated]!!,
                        icon = R.drawable.ic_checkmark
                    )
                    StepText(text = "Sent")

                    // Step 2: Processing
                    ProgressBar(
                        state = stepStates[ProgressStep.Processing]!!
                    )

                    StepIcon(
                        state = stepStates[ProgressStep.Processing]!!,
                        icon = when (stepStates[ProgressStep.Processing]!!.isFailed) {
                            true -> R.drawable.ic_failed
                            else -> R.drawable.ic_checkmark
                        }
                    )
                    StepText(text = "Received")

                    // Step 3: Completed
                    ProgressBar(
                        state = stepStates[ProgressStep.Completed]!!
                    )
                },
                measurePolicy = { measurables, constraints ->
                    // Assign measurables to their respective elements
                    val progress1 = measurables[0]
                    val icon1 = measurables[1]
                    val text1 = measurables[2]
                    val progress2 = measurables[3]
                    val icon2 = measurables[4]
                    val text2 = measurables[5]
                    val progress3 = measurables[6]

                    val rowSpacing = 4.dp.toPx().roundToInt()
                    val columnSpacing = 5.dp.toPx().roundToInt()

                    // Measure icons with their intrinsic constraints
                    val icon1Placeable = icon1.measure(constraints.copy(minWidth = 0))
                    val icon2Placeable = icon2.measure(constraints.copy(minWidth = 0))

                    // Calculate progress widths based on weights
                    val availableWidth =
                        constraints.maxWidth - (icon1Placeable.width + icon2Placeable.width + 4 * rowSpacing)
                    val progressBar1Width = (availableWidth * 0.18f).toInt()
                    val progressBar2Width = (availableWidth * 0.64f).toInt()
                    val progressBar3Width = (availableWidth * 0.18f).toInt()

                    // Measure progress bars with calculated widths
                    val progressBar1Placeable = progress1.measure(
                        constraints.copy(minWidth = progressBar1Width, maxWidth = progressBar1Width)
                    )
                    val progressBar2Placeable = progress2.measure(
                        constraints.copy(minWidth = progressBar2Width, maxWidth = progressBar2Width)
                    )
                    val progressBar3Placeable = progress3.measure(
                        constraints.copy(minWidth = progressBar3Width, maxWidth = progressBar3Width)
                    )

                    // Measure text elements with their intrinsic constraints
                    val text1Placeable = text1.measure(constraints.copy(minWidth = 0))
                    val text2Placeable = text2.measure(constraints.copy(minWidth = 0))


                    // Calculate the height of the layout
                    val progressHeight = maxOf(
                        progressBar1Placeable.height,
                        progressBar2Placeable.height,
                        progressBar3Placeable.height,
                        icon1Placeable.height,
                        icon2Placeable.height
                    )
                    val textHeight = maxOf(
                        text1Placeable.height,
                        text2Placeable.height
                    )
                    val totalHeight = progressHeight + textHeight + columnSpacing

                    // Calculate positions
                    val icon1X = progressBar1Width + rowSpacing
                    val progressBar2X = icon1X + icon1Placeable.width + rowSpacing
                    val icon2X = progressBar2X + progressBar2Width + rowSpacing
                    val progressBar3X = icon2X + icon2Placeable.width + rowSpacing

                    // Place the children
                    layout(constraints.maxWidth, totalHeight) {
                        val centerY = (progressHeight - progressBar1Placeable.height) / 2

                        progressBar1Placeable.placeRelative(
                            0,
                            centerY
                        )
                        icon1Placeable.placeRelative(
                            icon1X,
                            centerY - (icon1Placeable.height - progressBar1Placeable.height) / 2
                        )

                        progressBar2Placeable.placeRelative(progressBar2X, centerY)
                        icon2Placeable.placeRelative(
                            icon2X,
                            centerY - (icon2Placeable.height - progressBar1Placeable.height) / 2
                        )

                        progressBar3Placeable.placeRelative(progressBar3X, centerY)

                        text1Placeable.placeRelative(
                            x = icon1X + (icon1Placeable.width / 2) - (text1Placeable.width / 2),
                            y = icon1Placeable.height + columnSpacing
                        )
                        text2Placeable.placeRelative(
                            x = icon2X + (icon2Placeable.width / 2) - (text2Placeable.width / 2),
                            y = icon2Placeable.height + columnSpacing
                        )
                    }
                }
            )
        }
    )
}

@Composable
private fun ProgressBar(
    modifier: Modifier = Modifier,
    state: StepState
) {
    val infiniteTransition = rememberInfiniteTransition(label = "progress")
    val marqueePosition = infiniteTransition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800),
            repeatMode = RepeatMode.Restart
        ),
        label = "marquee"
    )

    val backgroundColor = if (state.isFailed) SdkColors.error else SdkColors.lightGreen
    val progressColor = when {
        state.isFailed -> SdkColors.error
        else -> SdkColors.success
    }

    Box(
        modifier = modifier
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor),
        content = {
            when {
                state.isCompleted -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(4.dp))
                            .background(progressColor)
                    )
                }

                state.isCurrent -> {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(),
                        content = {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth(0.4f)
                                    .align(Alignment.CenterStart)
                                    .offset(
                                        x = with(LocalDensity.current) {
                                            (marqueePosition.value * 170.dp).toPx()
                                        }.dp
                                    )
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(progressColor)
                            )
                        }
                    )
                }
            }
        }
    )
}

@Composable
private fun StepIcon(
    state: StepState,
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier
) {
    val opacity by animateFloatAsState(
        targetValue = if (state.isCompleted || state.isFailed) 1f else 0f,
        animationSpec = tween(300),
        label = "icon opacity"
    )
    val size by animateDpAsState(
        targetValue = 8.dp + (6.dp * opacity),
        animationSpec = tween(300),
        label = "icon size"
    )

    val iconColor = when {
        state.isFailed -> SdkColors.error
        state.isCompleted -> SdkColors.success
        else -> SdkColors.lightGreen
    }
    val backgroundColor by animateColorAsState(
        targetValue = if (opacity > 0.9f) iconColor else SdkColors.lightGreen,
        animationSpec = tween(300),
        label = "background color"
    )

    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
        content = {
            Image(
                painter = painterResource(id = icon),
                contentDescription = null,
                modifier = Modifier
                    .size(8.dp)
                    .alpha(opacity)
            )
        }
    )
}

@Composable
private fun StepText(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier,
        text = text,
        fontSize = 10.sp,
        color = SdkColors.subText,
    )
}

@Preview(showBackground = true)
@Composable
private fun CheckoutInitiatedBottomSheetContentPreview() = SdkTheme {
    CheckoutInitiatedBottomSheetContent(
        state = TransactionState.Initiated(),
        onDone = {}
    )
}

private enum class ProgressStep {
    Initiated, Processing, Completed
}

private data class StepState(
    val isCompleted: Boolean = false,
    val isCurrent: Boolean = false,
    val isFailed: Boolean = false
)