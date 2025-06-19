package com.mona.sdk.presentation.shared

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mona.sdk.R

@Composable
internal fun SecuredByMona(
    modifier: Modifier = Modifier,
    title: String? = null
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        content = {
            Text(
                text = title ?: "Secured by",
                fontSize = 14.sp,
                fontWeight = FontWeight.W500,
            )
            Icon(
                painter = painterResource(R.drawable.ic_logo_full),
                contentDescription = null
            )
        }
    )
}