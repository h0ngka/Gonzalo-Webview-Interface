package com.gonzalo.webviewinterface.presentation.webview

/**
 * WebViewScreen이 구독하는 지속적인 UI 상태.
 *
 * @param popupText null이 아니면 팝업(Dialog)이 표시된다.
 * @param isKeyboardVisible 숨김 EditText에 대한 포커스/소프트 키보드 표시 여부.
 */
data class WebViewUiState(
    val popupText: String? = null,
    val isKeyboardVisible: Boolean = false
)

/**
 * 한 번만 소비되어야 하는 일회성 UI 이벤트 (SharedFlow로 전달).
 */
sealed interface WebViewUiEvent {
    data class SetOrientation(val orientation: Int) : WebViewUiEvent
    data class ShowToast(val text: String) : WebViewUiEvent
    data object RequestLocationPermission : WebViewUiEvent
    data class LocationResult(val json: String) : WebViewUiEvent
}
