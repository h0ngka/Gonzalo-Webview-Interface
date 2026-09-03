package com.gonzalo.webviewinterface.data.bridge

/**
 * [WebAppInterface]가 수신한 JS 호출을 앱 상위 레이어(presentation)로 전달하기 위한 콜백 계약.
 * WebViewViewModel이 이 인터페이스를 구현하여 UseCase 호출 및 UI 상태 갱신을 담당한다.
 */
interface WebBridgeListener {
    fun onSetOrientation(mode: String)
    fun onShowKeyboard()
    fun onHideKeyboard()
    fun onShowPopup(text: String)
    fun onShowToast(text: String)
    fun onGetLocation()
}
