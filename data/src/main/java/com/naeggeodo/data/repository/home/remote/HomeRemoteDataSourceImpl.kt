package com.naeggeodo.data.repository.home.remote

import com.naeggeodo.data.api.CategoryApi
import com.naeggeodo.data.api.InfoApi
import com.naeggeodo.data.api.SearchChatListByCategoryApi
import com.naeggeodo.data.base.BaseRepository
import com.naeggeodo.domain.model.Categories
import com.naeggeodo.domain.model.ChatList
import com.naeggeodo.domain.model.MyNickName
import com.naeggeodo.domain.utils.RemoteErrorEmitter
import timber.log.Timber
import javax.inject.Inject

class HomeRemoteDataSourceImpl @Inject constructor(
    private val categoryApi: CategoryApi,
    private val searchChatListByCategoryApi: SearchChatListByCategoryApi,
    private val infoApi: InfoApi
) : HomeRemoteDataSource, BaseRepository() {
    override suspend fun getCategories(
        remoteErrorEmitter: RemoteErrorEmitter
    ): Categories? {
        val res = safeApiCall(remoteErrorEmitter) {
            categoryApi.getCategories()
        }
        return if (res != null && res.isSuccessful && res.code() == 200) {
            res.body()
        } else {
            Timber.e("Api call failed / status:${res?.code()} errorBody:${res?.errorBody()}")
            null
        }
    }

    override suspend fun getChatList(
        remoteErrorEmitter: RemoteErrorEmitter,
        category: String?,
        buildingCode: String
    ): ChatList? {
        val res = safeApiCall(remoteErrorEmitter) {
            searchChatListByCategoryApi.getChatList(category, buildingCode)
        }
        return if (res != null && res.isSuccessful && res.code() == 200) {
            res.body()
        } else {
            Timber.e("Api call failed / status:${res?.code()} errorBody:${res?.errorBody()}")
            null
        }
    }

    override suspend fun getMyNickName(
        remoteErrorEmitter: RemoteErrorEmitter,
        userId: String
    ): MyNickName? {

        val res = safeApiCall(remoteErrorEmitter) {
            infoApi.getMyNickName(userId)
        }
        return if (res != null && res.isSuccessful && res.code() == 200) {
            res.body()
        } else {
            Timber.e("Api call failed / status:${res?.code()} errorBody:${res?.errorBody()}")
            null
        }
    }
}