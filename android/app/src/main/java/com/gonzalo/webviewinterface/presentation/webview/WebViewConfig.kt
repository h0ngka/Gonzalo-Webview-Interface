package com.gonzalo.webviewinterface.presentation.webview

/**
 * WebView가 로드할 대상 URL을 정의하는 단일 상수 파일.
 *
 * Vercel 배포가 완료되면 [TARGET_URL] 값만 실제 배포 URL로 교체하면 된다.
 * 앱 내 다른 모든 코드는 이 상수를 참조만 하고, URL을 직접 하드코딩하지 않는다.
 */
object WebViewConfig {

    const val TARGET_URL: String = "https://temporary-speedy-tempest-lhd12dt.vercel.app/"
}
