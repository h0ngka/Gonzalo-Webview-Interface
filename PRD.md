# WebView Interface 테스트 앱 PRD

- 문서 상태: Draft v1.0
- 작성일: 2026-09-03
- 작성자: product-manager

## 1. 제품 개요 / 목적

Android WebView와 웹 페이지 간의 네이티브 브리지(JavaScript Bridge) 기능을 테스트/검증하기 위한 참조용 앱이다. 웹 페이지에서 버튼을 클릭하면 JS Bridge를 통해 Android 네이티브 기능이 호출되어 동작하며, 이를 통해 웹↔앱 통신 구조의 정합성과 안정성을 검증한다.

이 프로젝트는 모노레포로 구성되며, `web/`(바닐라 HTML/CSS/JS, Vercel 배포)과 `android/`(Kotlin, Jetpack Compose, Clean Architecture)가 하나의 JS Bridge 인터페이스로 연결된다.

## 2. 목표 및 성공 지표

### 목표
- 5개 핵심 기능이 웹 → 앱 → (필요 시 앱 → 웹) 방향으로 정상 왕복 동작한다.
- 웹 페이지는 Vercel에 배포되어 실제 URL로 Android WebView에서 로드 가능해야 한다.
- Android 앱은 Clean Architecture 원칙에 따라 유지보수 가능한 구조로 구현되어야 한다.

### 성공 지표 (Definition of Done 기준)
- 5개 기능 모두 실기기/에뮬레이터에서 QA 테스트 통과 (정상 케이스 + 주요 예외 케이스)
- 기능 호출 중 앱 크래시 0건
- 위치 정보 등 권한이 필요한 기능에서 권한 거부 시에도 앱이 크래시 없이 사용자에게 적절히 안내
- 웹에서 전달한 텍스트(팝업/토스트)가 인코딩 손실이나 XSS성 오류 없이 그대로 노출
- 각 기능의 웹→앱 호출부터 네이티브 동작 완료까지 체감 지연 없이(1초 이내) 반응

## 3. 아키텍처 개요

### 통신 구조
- Android `WebView`에 `addJavascriptInterface()`로 네이티브 객체를 등록하고, 웹에서는 `window.AndroidBridge.xxx()` 형태의 전역 인터페이스를 통해 네이티브 기능을 호출한다.
- 네이티브 → 웹 방향의 응답(예: 위치 조회 결과 등)이 필요한 경우, `WebView.evaluateJavascript()`로 웹의 전역 콜백 함수(예: `window.onLocationResult(jsonString)`)를 직접 호출하는 방식으로 확정한다.
- 전달 데이터 포맷은 JSON 문자열을 기본으로 하며, 브리지 메서드는 `String` 파라미터를 받는 것을 원칙으로 한다 (WebView JS Interface는 기본 타입만 안전하게 지원).

### 제안 JS Bridge 인터페이스 (초안 — 세부 구현 시 web/android 담당자 간 최종 조정 가능)

```js
// 1. 화면 방향 전환
window.AndroidBridge.setOrientation(mode) // mode: "portrait" | "landscape"

// 2. 키보드 노출/비노출
window.AndroidBridge.showKeyboard()
window.AndroidBridge.hideKeyboard()

// 3. 텍스트 팝업(다이얼로그) 노출
window.AndroidBridge.showPopup(text)

// 4. 텍스트 토스트 노출
window.AndroidBridge.showToast(text)

// 5. 위치 정보 조회 후 팝업 노출
window.AndroidBridge.getLocation()
// 위치 조회는 비동기이므로 결과는 콜백으로 전달: evaluateJavascript()가
// window.onLocationResult(jsonString) 을 호출하는 방식으로 확정
```

## 4. 디렉토리 구조 제안

```
Gonzalo-Webview-Interface/
├── PRD.md
├── web/
│   ├── index.html
│   ├── css/
│   │   └── style.css
│   ├── js/
│   │   ├── main.js
│   │   └── bridge.js        # AndroidBridge 호출 래핑 및 콜백 처리
│   └── vercel.json
└── android/
    ├── app/
    │   ├── src/main/java/com/gonzalo/webviewinterface/
    │   │   ├── presentation/    # Compose UI, ViewModel, WebView 화면
    │   │   │   ├── webview/
    │   │   │   └── MainActivity.kt
    │   │   ├── domain/          # UseCase, Repository 인터페이스, 모델
    │   │   │   ├── usecase/
    │   │   │   └── model/
    │   │   ├── data/            # Repository 구현체, 브리지/시스템 API 연동
    │   │   │   ├── bridge/      # JavascriptInterface 구현체
    │   │   │   ├── location/
    │   │   │   └── repository/
    │   │   └── di/              # Hilt 모듈
    │   └── build.gradle.kts
    └── build.gradle.kts
```

## 5. 기능별 상세 요구사항

### 5.1 WebView 가로/세로 모드 변경
- **트리거**: 웹 화면의 "가로 모드" / "세로 모드" 버튼 클릭
- **웹 → 앱 전달 데이터**: 모드 문자열 (예: `"portrait"` / `"landscape"`), 또는 방향별 버튼 2개로 분리 가능 (web 담당자 재량)
- **앱 동작**: `Activity.requestedOrientation`을 변경하여 화면 방향 전환
- **필요 권한**: 없음 (단, `AndroidManifest.xml`에 `android:configChanges="orientation|screenSize"` 등 설정 검토 필요)
- **예외/에러 케이스**:
  - 잘못된 파라미터 전달 시 무시하고 로그만 남김 (크래시 방지)
  - 기기가 특정 방향을 지원하지 않는 경우(폴더블 등) 대응은 QA 단계에서 실기기 확인

### 5.2 키보드 노출/비노출
- **트리거**: 웹 화면의 "키보드 표시" / "키보드 숨김" 버튼 클릭
- **웹 → 앱 전달 데이터**: 없음 (별도 함수 2개 호출) 또는 boolean 플래그
- **앱 동작**: WebView 컨테이너에 크기 0(화면 밖)의 숨김 `EditText`를 두고, 표시 시 해당 `EditText`에 포커스를 주어 `InputMethodManager.showSoftInput()`을 트리거하는 방식으로 확정. 숨길 때는 포커스를 해제하고 `InputMethodManager.hideSoftInputFromWindow()`를 호출한다.
- **필요 권한**: 없음
- **예외/에러 케이스**: 포커스 가능한 뷰가 없을 때 안전하게 무시, 크래시 없이 로그 처리

### 5.3 전달한 텍스트 팝업 노출
- **트리거**: 웹에서 텍스트 입력 후 "팝업으로 보기" 버튼 클릭
- **웹 → 앱 전달 데이터**: 사용자가 입력한 문자열 텍스트
- **앱 동작**: `AlertDialog`(또는 Compose Dialog)로 전달받은 텍스트를 표시
- **필요 권한**: 없음
- **예외/에러 케이스**:
  - 빈 문자열 전달 시 기본 안내 문구로 대체 표시
  - 매우 긴 텍스트(예: 수천 자) 전달 시 스크롤 가능한 다이얼로그로 처리하거나 길이 제한 적용
  - 특수문자/이모지/HTML 태그 포함 텍스트도 안전하게(이스케이프 없이 원문 그대로) 표시

### 5.4 전달한 텍스트 토스트 메시지 노출
- **트리거**: 웹에서 텍스트 입력 후 "토스트로 보기" 버튼 클릭
- **웹 → 앱 전달 데이터**: 사용자가 입력한 문자열 텍스트
- **앱 동작**: `Toast.makeText()`로 전달받은 텍스트 노출 (길이: SHORT 기본, 필요 시 LONG 옵션 검토)
- **필요 권한**: 없음
- **예외/에러 케이스**:
  - 빈 문자열 전달 시 토스트 미노출 또는 기본 문구 처리
  - 연속 클릭 시 토스트 큐잉/중복 노출 이슈 QA에서 확인

### 5.5 위치 정보 조회 후 팝업 노출
- **트리거**: 웹 화면의 "위치 정보 확인" 버튼 클릭
- **웹 → 앱 전달 데이터**: 없음 (호출만 트리거)
- **앱 동작**:
  1. 위치 권한(`ACCESS_FINE_LOCATION` 및/또는 `ACCESS_COARSE_LOCATION`) 보유 여부 확인
  2. 미보유 시 런타임 권한 요청 다이얼로그 노출
  3. 권한 승인 시 `FusedLocationProviderClient` 등을 통해 현재 위치(위도/경도) 조회
  4. 조회 결과를 `AlertDialog`(또는 Compose Dialog)로 팝업 표시
- **필요 권한**: `ACCESS_FINE_LOCATION`, `ACCESS_COARSE_LOCATION` (Manifest 등록 + 런타임 요청)
- **예외/에러 케이스**:
  - 사용자가 권한을 거부한 경우: 크래시 없이 "권한이 필요합니다" 안내 팝업/토스트 표시
  - 사용자가 "다시 묻지 않음"을 선택해 영구 거부한 경우: 설정 화면 이동 딥링크는 범위 제외(Out of Scope)이며, 안내 메시지(토스트/다이얼로그)만 노출하는 것으로 확정
  - 위치 서비스(GPS/네트워크)가 꺼져 있는 경우: 위치 조회 실패 안내
  - 위치 조회 타임아웃: 일정 시간 내 응답 없으면 실패 처리 및 안내 (타임아웃 값은 android 개발자가 합리적으로 결정, 예: 10초)

## 6. 비기능 요구사항

- **지원 Android 버전**: minSdk 24 (Android 7.0), targetSdk/compileSdk 35로 확정
- **applicationId**: `com.gonzalo.webviewinterface`로 확정
- **권한 처리**: 런타임 권한이 필요한 기능(위치)은 반드시 권한 요청 → 거부 시 안내의 표준 플로우를 따른다. 권한 관련 UX는 Android 공식 가이드라인 준수
- **에러 핸들링**: 모든 JS Bridge 호출은 잘못된 입력(null, 빈 문자열, 타입 불일치 등)에도 앱이 크래시하지 않도록 방어적으로 구현
- **웹-앱 결합도**: 웹 페이지는 Android 브리지 객체(`window.AndroidBridge`)가 없는 환경(일반 브라우저)에서 실행될 경우에도 에러 없이 동작(기능 비활성/안내 처리)하도록 방어 코드 작성 권장
- **보안**: JS Bridge로 노출하는 메서드는 필요한 최소 범위로 제한 (`@JavascriptInterface` 어노테이션 사용, 불필요한 메서드 노출 금지)
- **로깅**: 브리지 호출/응답에 대한 디버그 로그를 남겨 QA 및 트러블슈팅 지원
- **성능**: 브리지 호출 후 네이티브 동작(다이얼로그/토스트 노출 등)까지 체감 지연 없이(1초 이내) 반응

## 7. 역할 및 담당

| 역할 | 담당 범위 |
|---|---|
| product-manager | PRD 작성 및 유지보수, 전체 일정 관리, 우선순위/의존관계 조율 |
| frontend-developer | `web/` 웹 애플리케이션 개발(바닐라 HTML/CSS/JS), JS Bridge 호출부 구현, Vercel 배포 |
| android-developer | `android/` Android 앱 개발, WebView 설정, JS Bridge(`@JavascriptInterface`) 구현, Clean Architecture(presentation/domain/data) 적용, Hilt/Coroutine/ViewModel 구성 |
| qa-engineer | 5개 기능의 기능 테스트, 에러/예외 케이스 검증, 권한 거부 등 엣지 케이스 확인, 배포 전 최종 검증 |

## 8. 마일스톤 / 일정 (제안)

아래는 상대적 순서와 의존관계 기준의 제안 일정이다. 리소스/일정은 진행 상황에 따라 유동적으로 조정한다.

| 단계 | 내용 | 선행 조건 | 담당 |
|---|---|---|---|
| 1. PRD 확정 | 본 PRD 검토 및 최종 확정 | - | product-manager (+ 전체 리뷰) |
| 2. 인터페이스 협의 | web/android 간 JS Bridge 함수 시그니처, 콜백 포맷 최종 확정 | 1단계 완료 | frontend-developer, android-developer |
| 3. web 개발 | `web/` 스캐폴딩, 5개 기능 UI 및 브리지 호출부 구현, Vercel 배포 | 2단계 완료 | frontend-developer |
| 4. android 개발 | `android/` 프로젝트 구성(Clean Architecture, Hilt), WebView 설정, JS Bridge 구현, 5개 기능 네이티브 로직 구현 | 2단계 완료 (3단계와 병렬 진행 가능, WebView 로드 URL은 3단계 배포 URL 필요) | android-developer |
| 5. 통합 및 QA | web 배포 URL을 android WebView에 연동, 5개 기능 통합 테스트, 예외 케이스 검증 | 3, 4단계 완료 | qa-engineer (+ frontend/android 지원) |
| 6. 배포 준비 | QA 이슈 수정 반영, 최종 검증 후 배포(내부 배포/테스트 빌드) | 5단계 완료 | 전체 |

## 9. Out of Scope

- 앱 → 웹 방향의 복잡한 양방향 데이터 동기화(예: 실시간 상태 공유, 대용량 데이터 전송)는 범위 밖. 본 앱은 버튼 클릭 기반의 단방향 트리거 + 필요 시 단순 결과 콜백 수준만 다룬다.
- 위치 권한 영구 거부 시 설정 화면으로 이동시키는 딥링크 기능 (안내 메시지 노출까지만 구현)
- 로그인/인증, 사용자 계정 관리 기능
- iOS WebView 대응 (본 프로젝트는 Android 전용)
- 오프라인 모드 지원 (웹 페이지는 Vercel 배포 URL을 온라인으로 로드하는 것을 전제)
- 다국어(i18n) 지원
- 위치 정보 이외의 추가 네이티브 기능(카메라, 파일 업로드, 푸시 알림 등) — 향후 확장 시 별도 PRD로 다룸
- 자동화된 CI/CD 파이프라인 구축 (배포는 수동 프로세스로 우선 진행, 추후 별도 논의)
- 앱 스토어(Google Play) 정식 출시 프로세스 (본 앱은 테스트/검증 목적의 내부용 앱)
