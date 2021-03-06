package com.naeggeodo.domain.usecase

import com.naeggeodo.domain.repository.InfoRepository
import com.naeggeodo.domain.utils.RemoteErrorEmitter
import javax.inject.Inject

class GetMyInfoUseCase @Inject constructor(
    private val infoRepository: InfoRepository
) {
    suspend fun execute(remoteErrorEmitter: RemoteErrorEmitter, userId: String) =
        infoRepository.getMyInfo(remoteErrorEmitter, userId)
}
