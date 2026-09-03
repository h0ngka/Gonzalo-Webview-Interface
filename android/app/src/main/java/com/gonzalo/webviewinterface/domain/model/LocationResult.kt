package com.gonzalo.webviewinterface.domain.model

/**
 * 위치 조회 결과를 나타내는 도메인 모델.
 */
sealed interface LocationResult {
    data class Success(val latitude: Double, val longitude: Double) : LocationResult
    data class Failure(val errorCode: LocationErrorCode) : LocationResult
}

/**
 * 위치 조회 실패 사유 코드. 웹으로 전달되는 JSON의 "error" 필드 값으로 그대로 사용된다.
 */
enum class LocationErrorCode {
    PERMISSION_DENIED,
    LOCATION_DISABLED,
    TIMEOUT,
    UNKNOWN
}
