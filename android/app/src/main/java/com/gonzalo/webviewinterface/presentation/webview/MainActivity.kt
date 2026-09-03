package com.gonzalo.webviewinterface.presentation.webview

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint

/**
 * 앱의 단일 진입점. WebViewScreen 하나만 호스팅한다.
 *
 * AndroidManifest.xml에서 android:configChanges="orientation|screenSize|screenLayout|keyboardHidden"를
 * 지정했기 때문에, setOrientation() 브리지 호출로 방향이 바뀌어도 액티비티가 재생성되지 않는다.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    WebViewScreen()
                }
            }
        }
    }
}
