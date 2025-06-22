package com.mona.sdk.presentation.bottomsheet

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mona.sdk.presentation.theme.SdkColors

@Composable
fun LoadingBottomSheetContent(
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotation = infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    val primary = MaterialTheme.colorScheme.primary
    val size = 70.dp

    // Calculate content size accounting for padding
    val contentSize = size - (7.dp * 2)

    Column(
        modifier = modifier.heightIn(200.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        content = {
            Box(
                modifier = Modifier
                    .size(size)
                    .background(primary.copy(alpha = 0.1f), shape = CircleShape),
                contentAlignment = Alignment.Center,
                content = {
                    Box(
                        modifier = Modifier
                            .size(contentSize)
                            .rotate(rotation.value),
                        contentAlignment = Alignment.Center,
                        content = {
                            Canvas(
                                modifier = Modifier.matchParentSize(),
                                onDraw = {
                                    val sweepAngle = (30f / 100f) * 360f

                                    drawArc(
                                        color = primary,
                                        startAngle = -90f, // Start from top
                                        sweepAngle = sweepAngle,
                                        useCenter = false,
                                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round),
                                    )
                                }
                            )
                        }
                    )
                }
            )
            Text(
                modifier = Modifier.padding(top = 16.dp),
                text = "Processing",
                fontSize = 16.sp,
                color = SdkColors.subText,
            )
        }
    )
}