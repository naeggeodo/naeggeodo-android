package com.naeggeodo.domain.usecase

import com.naeggeodo.domain.repository.LoginRepository
import com.naeggeodo.domain.utils.RemoteErrorEmitter
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val loginRepository: LoginRepository
) {
    suspend fun execute(
        remoteErrorEmitter: RemoteErrorEmitter,
        platform: String,
        body: HashMap<String, String?>
    ) = loginRepository.logIn(remoteErrorEmitter, platform, body)
}