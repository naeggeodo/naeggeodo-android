package com.naeggeodo.data.repository.home.remote

import com.naeggeodo.data.api.CategoryApi
import com.naeggeodo.data.api.MyInfoApi
import com.naeggeodo.data.api.SearchChatListByCategoryApi
import com.naeggeodo.data.base.BaseRepository
import com.naeggeodo.domain.model.Categories
import com.naeggeodo.domain.model.ChatList
import com.naeggeodo.domain.model.MyInfo
import com.naeggeodo.domain.utils.RemoteErrorEmitter
import timber.log.Timber
import javax.inject.Inject

class HomeRemoteDataSourceImpl @Inject constructor(
    private val categoryApi: CategoryApi,
    private val searchChatListByCategoryApi: SearchChatListByCategoryApi,
    private val myInfoApi: MyInfoApi
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

    override suspend fun getMyInfo(
        remoteErrorEmitter: RemoteErrorEmitter,
        userId: String
    ): MyInfo? {

        val res = safeApiCall(remoteErrorEmitter) {
            myInfoApi.getMyInfo(userId)
        }
        return if (res != null && res.isSuccessful && res.code() == 200) {
            res.body()
        } else {
            Timber.e("Api call failed / status:${res?.code()} errorBody:${res?.errorBody()}")
            null
        }
    }
}