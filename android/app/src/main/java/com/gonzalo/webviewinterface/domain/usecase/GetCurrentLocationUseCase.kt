package com.gonzalo.webviewinterface.domain.usecase

import com.gonzalo.webviewinterface.domain.model.LocationResult
import com.gonzalo.webviewinterface.domain.repository.LocationRepository
import javax.inject.Inject

/**
 * 현재 위치를 조회하는 유스케이스.
 * 권한 확인은 호출부(ViewModel)에서 선행되어야 하며, 이 유스케이스는 조회 자체에 집중한다.
 */
class GetCurrentLocationUseCase @Inject constructor(
    private val locationRepository: LocationRepository
) {
    suspend operator fun invoke(): LocationResult = locationRepository.getCurrentLocation()
}
