package com.example.andalib.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.andalib.ui.theme.AndalibDarkBlue
import com.example.andalib.ui.theme.AndalibGray
import com.example.andalib.ui.theme.AndalibLightBlue
import com.example.andalib.ui.theme.AndalibWhite

@Composable
fun AndalibTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default.copy(
        imeAction = ImeAction.Next
    ),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AndalibDarkBlue,
            focusedLabelColor = AndalibDarkBlue,
            cursorColor = AndalibDarkBlue,
            unfocusedBorderColor = AndalibGray.copy(alpha = 0.5f)
        ),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions
    )
}

@Composable
fun AndalibPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default.copy(
        keyboardType = KeyboardType.Password,
        imeAction = ImeAction.Done
    ),
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = keyboardOptions,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AndalibDarkBlue,
            focusedLabelColor = AndalibDarkBlue,
            cursorColor = AndalibDarkBlue,
            unfocusedBorderColor = AndalibGray.copy(alpha = 0.5f)
        ),
        keyboardActions = keyboardActions
    )
}

@Composable
fun AndalibButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AndalibDarkBlue),
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = text,
            color = AndalibWhite,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun ClickableAuthText(
    prefixText: String,
    clickableText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Text(
            text = prefixText,
            color = AndalibGray
        )
        TextButton(
            onClick = onClick,
            contentPadding = PaddingValues(start = 4.dp, end = 4.dp)
        ) {
            Text(
                text = clickableText,
                color = AndalibLightBlue,
                fontWeight = FontWeight.Bold
            )
        }
    }
}