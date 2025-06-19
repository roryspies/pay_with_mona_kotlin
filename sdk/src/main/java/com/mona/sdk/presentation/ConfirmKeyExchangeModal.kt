package com.mona.sdk.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mona.sdk.R
import com.mona.sdk.presentation.shared.SdkButton
import com.mona.sdk.presentation.shared.SecuredByMona
import com.mona.sdk.presentation.theme.SdkTheme
import com.mona.sdk.util.appIconPainter

@Composable
internal fun ConfirmKeyExchangeModal(
    modifier: Modifier = Modifier,
    onUserDecision: (Boolean) -> Unit
) {
    Column(
        modifier = modifier
            .padding(bottom = 8.dp)
            .navigationBarsPadding()
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp)
            )
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = {
            Column(
                modifier = Modifier.padding(16.dp)
                    .background(MaterialTheme.colorScheme.background, RoundedCornerShape(8.dp))
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                content = {
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth(),
                        content = {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(
                                            alpha = 0.1f
                                        )
                                    ),
                                contentAlignment = Alignment.Center,
                                content = {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_logo),
                                        contentDescription = null,
                                    )
                                }
                            )
                            Icon(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                painter = painterResource(R.drawable.path_checkmark),
                                contentDescription = null,
                            )
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(
                                            alpha = 0.1f
                                        )
                                    ),
                                contentAlignment = Alignment.Center,
                                content = {
                                    val painter = appIconPainter()
                                    if (painter != null) {
                                        Image(
                                            painter = painter,
                                            contentDescription = null,
                                        )
                                    }
                                }
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "One Last Thing! ",
                        fontSize = MaterialTheme.typography.titleLarge.fontSize,
                        fontWeight = FontWeight.W600
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Set up biometrics for faster, one-tap \npayments — every time you check out.",
                        fontSize = MaterialTheme.typography.bodyLarge.fontSize,
                        color = Color(0xFF6A6C60),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth(),
                        content = {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                content = {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_safe),
                                        contentDescription = null,
                                        tint = Color(0xFF8E94F1)
                                    )
                                    Text(
                                        text = "This is to make sure that you are the only one who can authorize payments.",
                                        color = Color(0xFF8E94F1),
                                        modifier = Modifier.weight(1f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.W500,
                                        lineHeight = 16.sp,
                                    )
                                }
                            )
                        }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    SdkButton(
                        modifier = Modifier.fillMaxWidth(),
                        text = "Set Up",
                        onClick = { onUserDecision(true) }
                    )
                }
            )

            SecuredByMona(modifier = Modifier.padding(bottom = 8.dp))
        }
    )
}

@Preview
@Composable
private fun ConfirmKeyExchangeModalPreview() = SdkTheme {
    ConfirmKeyExchangeModal(
        onUserDecision = {}
    )
}