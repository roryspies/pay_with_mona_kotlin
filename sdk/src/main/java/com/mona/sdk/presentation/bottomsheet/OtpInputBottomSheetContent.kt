package com.mona.sdk.presentation.bottomsheet

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mona.sdk.presentation.shared.SdkButton
import com.mona.sdk.presentation.theme.SdkColors

@Composable
internal fun OtpInputBottomSheetContent(
    title: String,
    modifier: Modifier = Modifier,
    length: Int = 4,
    isPassword: Boolean = false,
    onDone: (String) -> Unit,
    onClose: () -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        content = {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )

            OtpInputField(
                length = length,
                modifier = Modifier
                    .padding(top = 22.dp, bottom = 8.dp)
                    .fillMaxWidth()
                    .background(SdkColors.white)
                    .padding(vertical = 32.dp, horizontal = 16.dp),
                isPassword = isPassword,
                onComplete = onDone
            )

            SdkButton(
                modifier = Modifier.fillMaxWidth(),
                text = "Close",
                onClick = onClose
            )
        }
    )
}

@Composable
private fun OtpInputField(
    length: Int,
    modifier: Modifier = Modifier,
    isPassword: Boolean = false,
    onComplete: (String) -> Unit
) {
    var otpValue by remember { mutableStateOf("") }
    var currentFocusIndex by remember { mutableStateOf(0) }
    val focusRequesters = remember { List(length) { FocusRequester() } }

    // Auto-focus first field when component loads
    LaunchedEffect(Unit) {
        if (focusRequesters.isNotEmpty()) {
            focusRequesters[0].requestFocus()
        }
    }

    // Check if OTP is complete
    LaunchedEffect(otpValue) {
        if (otpValue.length == length) {
            onComplete(otpValue)
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)
    ) {
        repeat(length) { index ->
            OtpDigitField(
                value = otpValue.getOrNull(index)?.toString() ?: "",
                isPassword = isPassword,
                isFocused = currentFocusIndex == index,
                focusRequester = focusRequesters[index],
                onValueChange = { newValue ->
                    handleOtpValueChange(
                        newValue = newValue,
                        currentValue = otpValue,
                        index = index,
                        length = length,
                        focusRequesters = focusRequesters,
                        onOtpChange = { otpValue = it },
                        onFocusChange = { currentFocusIndex = it }
                    )
                },
                onBackspace = {
                    handleBackspace(
                        currentValue = otpValue,
                        index = index,
                        focusRequesters = focusRequesters,
                        onOtpChange = { otpValue = it },
                        onFocusChange = { currentFocusIndex = it }
                    )
                },
                onFocusChanged = { isFocused ->
                    if (isFocused) {
                        currentFocusIndex = index
                    }
                },
                onClick = {
                    currentFocusIndex = index
                    focusRequesters[index].requestFocus()
                }
            )
        }
    }
}

@Composable
private fun OtpDigitField(
    value: String,
    isPassword: Boolean,
    isFocused: Boolean,
    focusRequester: FocusRequester,
    onValueChange: (String) -> Unit,
    onBackspace: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val borderColor by animateColorAsState(
        targetValue = when {
            isFocused || value.isNotEmpty() -> MaterialTheme.colorScheme.primary
            else -> Color.Transparent
        },
        label = "borderColor"
    )

    Box(
        modifier = Modifier
            .size(48.dp)
            .border(
                width = 2.dp,
                color = borderColor,
                shape = RoundedCornerShape(8.dp)
            )
            .background(
                color = MaterialTheme.colorScheme.surface,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
        content = {
            BasicTextField(
                value = TextFieldValue(
                    text = value,
                    selection = TextRange(value.length) // Always place cursor at end
                ),
                onValueChange = { textFieldValue ->
                    // Only take the last character if multiple characters are entered
                    onValueChange(textFieldValue.text.takeLast(1))
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.NumberPassword,
                    imeAction = ImeAction.Next
                ),
                visualTransformation = if (isPassword && value.isNotEmpty()) {
                    PasswordVisualTransformation()
                } else {
                    VisualTransformation.None
                },
                interactionSource = interactionSource,
                singleLine = true,
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        onFocusChanged(focusState.isFocused)
                    }
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.key == Key.Backspace && keyEvent.type == KeyEventType.KeyDown) {
                            onBackspace()
                            true
                        } else {
                            false
                        }
                    },
                textStyle = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.W500,
                    color = SdkColors.darkText,
                    textAlign = TextAlign.Center
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { innerTextField ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(48.dp),
                        content = { innerTextField() }
                    )
                }
            )
        }
    )
}

private fun handleOtpValueChange(
    newValue: String,
    currentValue: String,
    index: Int,
    length: Int,
    focusRequesters: List<FocusRequester>,
    onOtpChange: (String) -> Unit,
    onFocusChange: (Int) -> Unit
) {
    if (newValue.length <= 1 && newValue.all { it.isDigit() }) {
        val newOtp = currentValue.toMutableList().apply {
            // Ensure list is long enough
            while (size < length) add(' ')
            this[index] = newValue.firstOrNull() ?: ' '
        }.joinToString("").replace(" ", "")

        onOtpChange(newOtp)

        // Move to next field if we added a digit
        if (newValue.isNotEmpty() && index < length - 1) {
            val nextIndex = index + 1
            onFocusChange(nextIndex)
            focusRequesters[nextIndex].requestFocus()
        }
    }
}

private fun handleBackspace(
    currentValue: String,
    index: Int,
    focusRequesters: List<FocusRequester>,
    onOtpChange: (String) -> Unit,
    onFocusChange: (Int) -> Unit
) {
    if (index < currentValue.length) {
        // Delete current digit
        val newOtp = currentValue.removeRange(index, index + 1)
        onOtpChange(newOtp)
    } else if (index > 0) {
        // Move to previous field and delete its digit
        val prevIndex = index - 1
        val newOtp = if (prevIndex < currentValue.length) {
            currentValue.removeRange(prevIndex, prevIndex + 1)
        } else {
            currentValue
        }
        onOtpChange(newOtp)
        onFocusChange(prevIndex)
        focusRequesters[prevIndex].requestFocus()
    }
}

@Preview(showBackground = true)
@Composable
private fun OtpInputBottomSheetContentPreview() {
    OtpInputBottomSheetContent(
        title = "Enter OTP",
        onDone = { otp -> println("OTP Entered: $otp") },
        onClose = { println("Closed") }
    )
}