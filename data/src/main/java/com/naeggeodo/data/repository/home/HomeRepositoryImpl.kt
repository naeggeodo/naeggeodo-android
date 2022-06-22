package com.damda.data.repository.home

import com.damda.data.repository.home.remote.HomeRemoteDataSource
import com.naeggeodo.domain.model.Categories
import com.naeggeodo.domain.model.ChatList
import com.naeggeodo.domain.repository.HomeRepository
import com.naeggeodo.domain.utils.RemoteErrorEmitter
import javax.inject.Inject

class HomeRepositoryImpl @Inject constructor(
    private val homeRemoteDataSource: HomeRemoteDataSource
) : HomeRepository {
    override suspend fun getCategories(
        remoteErrorEmitter: RemoteErrorEmitter
    ): Categories? {
        return homeRemoteDataSource.getCategories(remoteErrorEmitter)
    }

    override suspend fun getChatList(
        remoteErrorEmitter: RemoteErrorEmitter,
        category: String?,
        buildingCode: String
    ): ChatList? {
        return homeRemoteDataSource.getChatList(remoteErrorEmitter, category, buildingCode)
    }
}