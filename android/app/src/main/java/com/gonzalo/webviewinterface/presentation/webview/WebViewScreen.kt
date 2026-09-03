package com.gonzalo.webviewinterface.presentation.webview

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import com.gonzalo.webviewinterface.data.bridge.WebAppInterface
import com.gonzalo.webviewinterface.presentation.components.PopupDialog
import kotlinx.coroutines.flow.collectLatest
import org.json.JSONObject

/**
 * WebView를 감싸는 메인 화면.
 * - WebViewConfig.TARGET_URL을 로드한다.
 * - AndroidBridge라는 이름으로 WebAppInterface를 등록해 웹 <-> 앱 통신을 연결한다.
 * - ViewModel의 상태(popup/keyboard)와 일회성 이벤트(orientation/toast/permission/location)를 구독한다.
 */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewScreen(
    viewModel: WebViewViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val uiState by viewModel.uiState.collectAsState()

    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onLocationPermissionResult(granted)
    }

    // 일회성 이벤트 처리: 화면 방향 전환 / 토스트 / 위치 권한 요청 / 위치 결과의 JS 콜백 전달
    LaunchedEffect(viewModel) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is WebViewUiEvent.SetOrientation -> {
                    activity?.requestedOrientation = event.orientation
                }

                is WebViewUiEvent.ShowToast -> {
                    Toast.makeText(context, event.text, Toast.LENGTH_SHORT).show()
                }

                WebViewUiEvent.RequestLocationPermission -> {
                    locationPermissionLauncher.launch(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        )
                    )
                }

                is WebViewUiEvent.LocationResult -> {
                    // JSONObject.quote()로 JS 문자열 리터럴 삽입 시 발생할 수 있는 인젝션을 방지한다.
                    val safeJsonLiteral = JSONObject.quote(event.json)
                    webViewRef?.evaluateJavascript(
                        "javascript:window.onLocationResult($safeJsonLiteral)",
                        null
                    )
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    webViewClient = WebViewClient()
                    addJavascriptInterface(WebAppInterface(viewModel), WebAppInterface.BRIDGE_NAME)
                    loadUrl(WebViewConfig.TARGET_URL)
                    webViewRef = this
                }
            }
        )

        HiddenKeyboardAnchor(isVisible = uiState.isKeyboardVisible)

        uiState.popupText?.let { text ->
            PopupDialog(text = text, onDismiss = viewModel::dismissPopup)
        }
    }
}

/**
 * 화면 밖(음수 offset)에 크기 1dp로 배치된 숨김 텍스트 필드.
 * showKeyboard/hideKeyboard 브리지 호출에 따라 포커스와 소프트 키보드 표시 여부를 제어한다.
 */
@Composable
private fun HiddenKeyboardAnchor(isVisible: Boolean) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var hiddenText by remember { mutableStateOf("") }

    BasicTextField(
        value = hiddenText,
        onValueChange = { hiddenText = it },
        modifier = Modifier
            .size(1.dp)
            .offset(x = (-10000).dp)
            .focusRequester(focusRequester)
    )

    LaunchedEffect(isVisible) {
        if (isVisible) {
            runCatching {
                focusRequester.requestFocus()
                keyboardController?.show()
            }
        } else {
            runCatching {
                keyboardController?.hide()
                focusManager.clearFocus(force = true)
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
