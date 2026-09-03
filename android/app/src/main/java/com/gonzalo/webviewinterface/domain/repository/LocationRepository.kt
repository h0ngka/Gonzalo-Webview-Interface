package com.gonzalo.webviewinterface.domain.repository

import com.gonzalo.webviewinterface.domain.model.LocationResult

/**
 * 현재 위치 조회를 위한 저장소 계약.
 * 실제 위치 API(FusedLocationProviderClient 등)와의 결합은 data 레이어에서 구현한다.
 */
interface LocationRepository {
    suspend fun getCurrentLocation(): LocationResult
}
