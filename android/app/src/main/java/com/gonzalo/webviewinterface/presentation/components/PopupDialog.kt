package com.gonzalo.webviewinterface.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.gonzalo.webviewinterface.R

/**
 * 웹에서 전달한 텍스트를 보여주는 공용 팝업.
 * 빈 문자열은 기본 안내 문구로 대체하고, 긴 텍스트는 스크롤 가능한 영역 안에 표시한다.
 */
@Composable
fun PopupDialog(
    text: String,
    onDismiss: () -> Unit
) {
    val displayText = text.ifBlank { stringResource(R.string.popup_default_text) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.popup_default_title)) },
        text = {
            Box(
                modifier = Modifier
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(text = displayText)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "확인")
            }
        }
    )
}
