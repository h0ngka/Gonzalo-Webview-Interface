package com.gonzalo.webviewinterface.presentation.webview

import android.Manifest
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gonzalo.webviewinterface.data.bridge.WebBridgeListener
import com.gonzalo.webviewinterface.domain.model.LocationErrorCode
import com.gonzalo.webviewinterface.domain.model.LocationResult
import com.gonzalo.webviewinterface.domain.usecase.GetCurrentLocationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.json.JSONObject
import javax.inject.Inject

/**
 * WebView 화면의 상태를 관리하고, JS Bridge([WebBridgeListener])로부터 전달받은 이벤트를
 * UseCase 호출 및 UI 상태(StateFlow)/일회성 이벤트(SharedFlow)로 변환한다.
 *
 * [WebAppInterface]의 각 `@JavascriptInterface` 메서드는 WebView 내부 스레드에서 호출될 수 있으므로,
 * 이 클래스의 콜백들은 스레드-세이프한 StateFlow.update / SharedFlow.tryEmit만 사용한다.
 */
@HiltViewModel
class WebViewViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val getCurrentLocationUseCase: GetCurrentLocationUseCase
) : ViewModel(), WebBridgeListener {

    private val _uiState = MutableStateFlow(WebViewUiState())
    val uiState: StateFlow<WebViewUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<WebViewUiEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<WebViewUiEvent> = _events.asSharedFlow()

    // ---- WebBridgeListener: JS Bridge -> ViewModel ----

    override fun onSetOrientation(mode: String) {
        val orientation = when (mode) {
            "landscape" -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            "portrait" -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            else -> {
                Log.w(TAG, "Unknown orientation mode: $mode - ignored")
                null
            }
        } ?: return
        _events.tryEmit(WebViewUiEvent.SetOrientation(orientation))
    }

    override fun onShowKeyboard() {
        _uiState.update { it.copy(isKeyboardVisible = true) }
    }

    override fun onHideKeyboard() {
        _uiState.update { it.copy(isKeyboardVisible = false) }
    }

    override fun onShowPopup(text: String) {
        _uiState.update { it.copy(popupText = text) }
    }

    override fun onShowToast(text: String) {
        _events.tryEmit(WebViewUiEvent.ShowToast(text))
    }

    override fun onGetLocation() {
        if (hasLocationPermission()) {
            fetchLocation()
        } else {
            _events.tryEmit(WebViewUiEvent.RequestLocationPermission)
        }
    }

    // ---- Compose UI -> ViewModel ----

    /** 권한 요청 결과를 Compose 쪽 launcher로부터 전달받는다. */
    fun onLocationPermissionResult(granted: Boolean) {
        if (granted) {
            fetchLocation()
        } else {
            handleLocationResult(LocationResult.Failure(LocationErrorCode.PERMISSION_DENIED))
        }
    }

    fun dismissPopup() {
        _uiState.update { it.copy(popupText = null) }
    }

    // ---- internal ----

    private fun hasLocationPermission(): Boolean {
        val fineGranted = ContextCompat.checkSelfPermission(
            appContext, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(
            appContext, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fineGranted || coarseGranted
    }

    private fun fetchLocation() {
        viewModelScope.launch {
            val result = runCatching { getCurrentLocationUseCase() }
                .getOrElse { e ->
                    Log.e(TAG, "getCurrentLocationUseCase() threw", e)
                    LocationResult.Failure(LocationErrorCode.UNKNOWN)
                }
            handleLocationResult(result)
        }
    }

    private fun handleLocationResult(result: LocationResult) {
        val popupMessage = when (result) {
            is LocationResult.Success ->
                "위치 조회 성공\n위도: ${result.latitude}\n경도: ${result.longitude}"
            is LocationResult.Failure ->
                "위치 조회 실패\n사유: ${result.errorCode.name}"
        }
        _uiState.update { it.copy(popupText = popupMessage) }
        _events.tryEmit(WebViewUiEvent.LocationResult(result.toJson()))
    }

    /** 웹으로 콜백할 JSON 문자열을 생성한다. org.json은 특수문자를 안전하게 이스케이프한다. */
    private fun LocationResult.toJson(): String {
        val json = JSONObject()
        when (this) {
            is LocationResult.Success -> {
                json.put("success", true)
                json.put("latitude", latitude)
                json.put("longitude", longitude)
            }
            is LocationResult.Failure -> {
                json.put("success", false)
                json.put("error", errorCode.name)
            }
        }
        return json.toString()
    }

    companion object {
        private const val TAG = "WebViewViewModel"
    }
}
