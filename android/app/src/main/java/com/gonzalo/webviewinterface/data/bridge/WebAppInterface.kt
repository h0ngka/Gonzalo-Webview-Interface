package com.gonzalo.webviewinterface.data.bridge

import android.util.Log
import android.webkit.JavascriptInterface
import java.util.Locale

/**
 * 웹 페이지(JS)에서 `window.AndroidBridge.xxx()` 형태로 호출하는 네이티브 진입점.
 *
 * WebView.addJavascriptInterface(WebAppInterface(listener), WebAppInterface.BRIDGE_NAME) 로 등록한다.
 *
 * 이 클래스는 최대한 얇게 유지한다: 입력값을 방어적으로 검증한 뒤 [WebBridgeListener]로 위임할 뿐,
 * UseCase 호출이나 UI 상태 관리 같은 로직은 갖지 않는다 (해당 로직은 presentation 레이어의
 * ViewModel이 담당한다).
 *
 * 주의: `@JavascriptInterface` 메서드는 WebView 내부 스레드(메인 스레드가 아닐 수 있음)에서 호출된다.
 * 따라서 UI를 직접 건드리지 않고, 스레드-세이프한 콜백(StateFlow/SharedFlow 갱신 등)만 수행한다.
 */
class WebAppInterface(private val listener: WebBridgeListener) {

    @JavascriptInterface
    fun setOrientation(mode: String?) {
        val safeMode = mode?.trim()?.lowercase(Locale.US)
        if (safeMode.isNullOrEmpty()) {
            Log.w(TAG, "setOrientation() called with null/empty mode")
            return
        }
        runCatching { listener.onSetOrientation(safeMode) }
            .onFailure { Log.e(TAG, "setOrientation() failed", it) }
    }

    @JavascriptInterface
    fun showKeyboard() {
        runCatching { listener.onShowKeyboard() }
            .onFailure { Log.e(TAG, "showKeyboard() failed", it) }
    }

    @JavascriptInterface
    fun hideKeyboard() {
        runCatching { listener.onHideKeyboard() }
            .onFailure { Log.e(TAG, "hideKeyboard() failed", it) }
    }

    @JavascriptInterface
    fun showPopup(text: String?) {
        val safeText = text ?: ""
        runCatching { listener.onShowPopup(safeText) }
            .onFailure { Log.e(TAG, "showPopup() failed", it) }
    }

    @JavascriptInterface
    fun showToast(text: String?) {
        if (text.isNullOrBlank()) {
            Log.d(TAG, "showToast() called with null/blank text - ignored")
            return
        }
        runCatching { listener.onShowToast(text) }
            .onFailure { Log.e(TAG, "showToast() failed", it) }
    }

    @JavascriptInterface
    fun getLocation() {
        runCatching { listener.onGetLocation() }
            .onFailure { Log.e(TAG, "getLocation() failed", it) }
    }

    companion object {
        private const val TAG = "WebAppInterface"

        /** WebView.addJavascriptInterface()에 등록할 객체 이름. 웹에서는 window.AndroidBridge로 접근한다. */
        const val BRIDGE_NAME = "AndroidBridge"
    }
}
