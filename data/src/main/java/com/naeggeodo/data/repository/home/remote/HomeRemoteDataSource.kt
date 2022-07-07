package com.naeggeodo.data.repository.home.remote

import com.naeggeodo.domain.model.Categories
import com.naeggeodo.domain.model.ChatList
import com.naeggeodo.domain.model.MyInfo
import com.naeggeodo.domain.utils.RemoteErrorEmitter

interface HomeRemoteDataSource {
    suspend fun getCategories(
        remoteErrorEmitter: RemoteErrorEmitter
    ): Categories?

    suspend fun getChatList(
        remoteErrorEmitter: RemoteErrorEmitter,
        category: String?,
        buildingCode: String
    ): ChatList?

    suspend fun getMyInfo(
        remoteErrorEmitter: RemoteErrorEmitter,
        userId: String
    ): MyInfo?
}